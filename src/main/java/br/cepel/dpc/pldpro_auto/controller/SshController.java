package br.cepel.dpc.pldpro_auto.controller;

import br.cepel.dpc.pldpro_auto.application.SshAccessUseCase;
import br.cepel.dpc.pldpro_auto.dto.AccessHostRequest;
import br.cepel.dpc.pldpro_auto.dto.AccessHostResponse;
import br.cepel.dpc.pldpro_auto.dto.MultiAccessHostRequest;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/ssh")
@Tag(name = "SSH", description = "Acesso remoto via SSH e execução de comandos")
public class SshController {

    private final SshAccessUseCase useCase;

    private static final int MAX_PARALLELISM = 20;

    public SshController(SshAccessUseCase useCase) { this.useCase = useCase; }

    @PostMapping("/exec")
    @Operation(
            summary = "Executa um comando via SSH",
            description = """
            Primeiro acesso: envie 'bootstrap=true' + 'password' para instalar a chave pública e acelerar conexões futuras.
            Próximos acessos: omita 'bootstrap' e 'password' para autenticar por chave.""",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AccessHostRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Primeiro acesso (instala chave)",
                                            value = """
                        {
                          "host": "10.0.0.12",
                          "port": 22,
                          "user": "pldpro",
                          "bootstrap": true,
                          "password": "senha_remota",
                          "command": "uname -a"
                        }"""
                                    ),
                                    @ExampleObject(
                                            name = "Acesso por chave (rápido)",
                                            value = """
                        {
                          "host": "10.0.0.12",
                          "port": 22,
                          "user": "pldpro",
                          "command": "ls -la"
                        }"""
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Comando executado",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AccessHostResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "Sucesso",
                                                    value = """
                            {
                              "exitCode": 0,
                              "stdout": "Linux host 6.8.0-... x86_64 GNU/Linux\\n",
                              "stderr": ""
                            }"""
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Requisição inválida (ex.: bootstrap=true sem password)"),
                    @ApiResponse(responseCode = "401", description = "Não autorizado"),
                    @ApiResponse(responseCode = "500", description = "Erro interno durante a conexão/execução")
            }
    )
    public ResponseEntity<AccessHostResponse> exec(
            @org.springframework.web.bind.annotation.RequestBody AccessHostRequest req) throws Exception {
        return ResponseEntity.ok(useCase.execute(req));
    }


    @Tag(name = "SSH", description = "Operações SSH em múltiplos caminhos")
    @Operation(
            summary = "Executa múltiplos comandos em paralelo",
            description = """
        Recebe uma lista de caminhos remotos (`paths`). Para cada caminho é montado o comando:
        {commandPrefix} {path} && PLDPro6.12.3-Container
        e executado em paralelo. Retorna uma lista de resultados na mesma ordem dos `paths`.
        Se `bootstrap=true` a primeira execução realiza o provisionamento da chave (se aplicável).
        """,
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MultiAccessHostRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Exemplo - com bootstrap",
                                            value = """
                    {
                      "host": "10.0.64.26",
                      "port": 22,
                      "user": "pldpro",
                      "commandPrefix": null,
                      "bootstrap": true,
                      "password": "senha_remota",
                      "paths": [
                        "/opt/app/dirA",
                        "/opt/app/dirB",
                        "/opt/app/dirC"
                      ]
                    }"""
                                    ),
                                    @ExampleObject(
                                            name = "Exemplo - sem bootstrap",
                                            value = """
                    {
                      "host": "10.0.64.26",
                      "port": 22,
                      "user": "pldpro",
                      "commandPrefix": null,
                      "bootstrap": false,
                      "password": null,
                      "paths": [
                        "/opt/app/dirX",
                        "/opt/app/dirY"
                      ]
                    }"""
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Resultados por caminho (mesma ordem da lista paths)",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = AccessHostResponse.class))
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Requisição inválida (ex.: lista paths vazia)"),
                    @ApiResponse(responseCode = "500", description = "Erro interno ao executar comandos SSH")
            }
    )
    @PostMapping("/multi-exec")
    public ResponseEntity<List<AccessHostResponse>> acionadorMultiCaso(
            @Valid @org.springframework.web.bind.annotation.RequestBody MultiAccessHostRequest req) throws Exception {

        List<String> paths = req.paths();
        if (paths == null || paths.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }

        int poolSize = Math.min(Math.max(1, paths.size()), MAX_PARALLELISM);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        try {
            List<CompletableFuture<AccessHostResponse>> futures = paths.stream()
                    .map(path -> CompletableFuture.supplyAsync(() -> {

                        String prefix = req.commandPrefix() == null ? "" : (req.commandPrefix().trim() + " ");

                        String command = prefix + path + " && PLDPro6.12.3-Container";

                        AccessHostRequest single = new AccessHostRequest(
                                req.host(),
                                req.port(),
                                req.user(),
                                command,
                                req.bootstrap(),
                                req.password()
                        );

                        try {
                            return useCase.execute(single);
                        } catch (Exception e) {
                            String err = e.getMessage() == null ? e.toString() : e.getMessage();
                            return new AccessHostResponse(-1, "", "EXCEPTION: " + err);
                        }
                    }, executor))
                    .collect(Collectors.toList());

            List<AccessHostResponse> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(results);

        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
