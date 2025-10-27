package com.securemessaging.controller;

import com.securemessaging.exception.KeyExchangeException;
import com.securemessaging.service.crypto.DiffieHellmanService;
import com.securemessaging.util.PRNGUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dh")
public class DiffieHellmanController {
    
    @Autowired
    private DiffieHellmanService dhService;
    
    // Endpoint para simular troca de chaves DH usando PRNG 128-bit
    @PostMapping("/simulate")
    public ResponseEntity<?> simulateDHExchange(@RequestBody Map<String, String> payload, Principal principal) {
        try {
            String userB = payload.get("userB");
            if (userB == null || userB.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "UserB is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            System.out.println("Iniciando simulação DH entre " + principal.getName() + " e " + userB);
            
            // Simular acordo de chaves DH
            DiffieHellmanService.DHSimulationResult result = dhService.simulateDHExchange(
                principal.getName(), userB
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userA", result.getUserA());
            response.put("userB", result.getUserB());
            response.put("privateKeyA", result.getPrivateKeyA().toString(16));
            response.put("publicKeyA", result.getPublicKeyA().toString(16));
            response.put("privateKeyB", result.getPrivateKeyB().toString(16));
            response.put("publicKeyB", result.getPublicKeyB().toString(16));
            response.put("sharedSecret", result.getSharedSecret().toString(16));
            response.put("sessionKeyAlgorithm", result.getSessionKey().getAlgorithm());
            response.put("sessionKeyLength", result.getSessionKey().getEncoded().length * 8);
            
            System.out.println("Simulação DH concluída com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (KeyExchangeException e) {
            System.err.println("Erro na simulação DH: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // Endpoint para demonstrar acordo de chaves DH
    @PostMapping("/exchange")
    public ResponseEntity<?> performDHExchange() {
        try {
            System.out.println("Executando troca de chaves DH real...");
            
            DiffieHellmanService.DHKeyExchangeResult result = dhService.performDHKeyExchange();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "DH Key Exchange completed successfully");
            response.put("privateValueA", result.getPrivateValueA().toString(16));
            response.put("privateValueB", result.getPrivateValueB().toString(16));
            response.put("publicValueB", result.getPublicValueB().toString(16));
            response.put("sharedSecretLength", result.getSharedSecret().getEncoded().length * 8);
            response.put("algorithm", result.getSharedSecret().getAlgorithm());
            
            System.out.println("Troca de chaves DH real concluída");
            
            return ResponseEntity.ok(response);
            
        } catch (KeyExchangeException e) {
            System.err.println("Erro na troca DH real: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // Endpoint para obter parâmetros DH
    @GetMapping("/parameters")
    public ResponseEntity<?> getDHParameters() {
        Map<String, Object> response = new HashMap<>();
        response.put("prime", "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381FFFFFFFFFFFFFFFF");
        response.put("generator", "2");
        response.put("keySize", 1024);
        response.put("description", "RFC 2409 - 1024-bit MODP Group");
        
        return ResponseEntity.ok(response);
    }
    
    // NOVO ENDPOINT: Teste completo do sistema DH
    @GetMapping("/test")
    public ResponseEntity<?> testDHImplementation() {
        try {
            System.out.println("=== TESTE COMPLETO DO SISTEMA DH ===");
            
            Map<String, Object> response = new HashMap<>();
            List<String> logs = new ArrayList<>();
            
            // 1. Testar PRNG 128-bit
            logs.add("1. Testando PRNG 128-bit...");
            BigInteger random1 = PRNGUtils.generate128BitRandom();
            BigInteger random2 = PRNGUtils.generate128BitRandom();
            
            boolean prngWorking = !random1.equals(random2);
            logs.add("   PRNG gera valores diferentes? " + prngWorking);
            logs.add("   Random 1: " + random1.toString(16));
            logs.add("   Random 2: " + random2.toString(16));
            logs.add("   Bit length random 1: " + random1.bitLength());
            logs.add("   Bit length random 2: " + random2.bitLength());
            
            // 2. Testar simulação DH
            logs.add("2. Testando simulação DH...");
            DiffieHellmanService.DHSimulationResult dhResult = 
                dhService.simulateDHExchange("testUserA", "testUserB");
            
            logs.add("   Usuários: " + dhResult.getUserA() + " <-> " + dhResult.getUserB());
            logs.add("   Chave privada A: " + dhResult.getPrivateKeyA().toString(16).substring(0, 32) + "...");
            logs.add("   Chave privada B: " + dhResult.getPrivateKeyB().toString(16).substring(0, 32) + "...");
            logs.add("   Chave pública A: " + dhResult.getPublicKeyA().toString(16).substring(0, 32) + "...");
            logs.add("   Chave pública B: " + dhResult.getPublicKeyB().toString(16).substring(0, 32) + "...");
            logs.add("   Segredo compartilhado: " + dhResult.getSharedSecret().toString(16).substring(0, 32) + "...");
            logs.add("   Tamanho do segredo: " + dhResult.getSharedSecret().bitLength() + " bits");
            logs.add("   Algoritmo da chave de sessão: " + dhResult.getSessionKey().getAlgorithm());
            logs.add("   Tamanho da chave de sessão: " + (dhResult.getSessionKey().getEncoded().length * 8) + " bits");
            
            // 3. Testar múltiplas execuções
            logs.add("3. Testando consistência em múltiplas execuções...");
            boolean consistent = true;
            for (int i = 0; i < 5; i++) {
                DiffieHellmanService.DHSimulationResult test = 
                    dhService.simulateDHExchange("userX", "userY");
                
                // Verificar se o segredo é sempre calculado corretamente
                if (test.getSharedSecret().bitLength() < 512) {
                    consistent = false;
                    logs.add("   ERRO: Segredo muito pequeno na execução " + (i+1) + 
                           " (" + test.getSharedSecret().bitLength() + " bits)");
                } else {
                    logs.add("   Execução " + (i+1) + ": Segredo = " + 
                           test.getSharedSecret().bitLength() + " bits - OK");
                }
            }
            logs.add("   Múltiplas execuções consistentes? " + consistent);
            
            // 4. Testar acordo de chaves real
            logs.add("4. Testando acordo de chaves real...");
            boolean realDHWorking = false;
            try {
                DiffieHellmanService.DHKeyExchangeResult realDH = dhService.performDHKeyExchange();
                logs.add("   Acordo de chaves real funcionou!");
                logs.add("   Tamanho chave compartilhada: " + realDH.getSharedSecret().getEncoded().length * 8 + " bits");
                logs.add("   Algoritmo: " + realDH.getSharedSecret().getAlgorithm());
                logs.add("   Chave privada A: " + realDH.getPrivateValueA().toString(16).substring(0, 20) + "...");
                logs.add("   Chave privada B: " + realDH.getPrivateValueB().toString(16).substring(0, 20) + "...");
                logs.add("   Chave pública B: " + realDH.getPublicValueB().toString(16).substring(0, 20) + "...");
                realDHWorking = true;
            } catch (Exception e) {
                logs.add("   ERRO no acordo real: " + e.getMessage());
                consistent = false;
            }
            
            // 5. Verificar parâmetros DH
            logs.add("5. Verificando parâmetros DH RFC 2409...");
            // Acessar constantes diretamente do serviço
            try {
                // Usar reflexão para acessar as constantes privadas
                java.lang.reflect.Field pField = DiffieHellmanService.class.getDeclaredField("DH_P");
                pField.setAccessible(true);
                BigInteger dhP = (BigInteger) pField.get(null);
                
                java.lang.reflect.Field gField = DiffieHellmanService.class.getDeclaredField("DH_G");
                gField.setAccessible(true);
                BigInteger dhG = (BigInteger) gField.get(null);
                
                logs.add("   Primo P válido? " + (dhP.bitLength() >= 1024) + " (" + dhP.bitLength() + " bits)");
                logs.add("   Gerador G válido? " + (dhG.equals(BigInteger.valueOf(2))) + " (G = " + dhG + ")");
                logs.add("   Primo P (primeiros 50 chars): " + dhP.toString(16).substring(0, 50) + "...");
                
            } catch (Exception e) {
                logs.add("   Erro ao verificar parâmetros: " + e.getMessage());
            }
            
            // 6. Teste de performance
            logs.add("6. Teste de performance...");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                dhService.simulateDHExchange("perfTest1", "perfTest2");
            }
            long endTime = System.currentTimeMillis();
            long avgTime = (endTime - startTime) / 10;
            logs.add("   Tempo médio por simulação: " + avgTime + "ms");
            logs.add("   Performance: " + (avgTime < 1000 ? "BOA" : "LENTA"));
            
            // 7. Teste de aleatoriedade
            logs.add("7. Teste de aleatoriedade do PRNG...");
            boolean randomnessOK = true;
            BigInteger[] randoms = new BigInteger[10];
            for (int i = 0; i < 10; i++) {
                randoms[i] = PRNGUtils.generate128BitRandom();
                for (int j = 0; j < i; j++) {
                    if (randoms[i].equals(randoms[j])) {
                        randomnessOK = false;
                        logs.add("   ERRO: Valores iguais encontrados!");
                        break;
                    }
                }
            }
            logs.add("   Aleatoriedade OK? " + randomnessOK);
            
            // Resultado final
            boolean allTestsPassed = prngWorking && consistent && realDHWorking && randomnessOK;
            
            response.put("success", allTestsPassed);
            response.put("message", allTestsPassed ? "TODOS OS TESTES PASSARAM! ✓" : "ALGUNS TESTES FALHARAM! ✗");
            response.put("prngWorking", prngWorking);
            response.put("consistent", consistent);
            response.put("realDHWorking", realDHWorking);
            response.put("randomnessOK", randomnessOK);
            response.put("averageExecutionTime", avgTime + "ms");
            response.put("logs", logs);
            response.put("dhResult", Map.of(
                "userA", dhResult.getUserA(),
                "userB", dhResult.getUserB(),
                "privateKeyA", dhResult.getPrivateKeyA().toString(16).substring(0, 32) + "... (128-bit)",
                "publicKeyA", dhResult.getPublicKeyA().toString(16).substring(0, 32) + "... (1024-bit)",
                "privateKeyB", dhResult.getPrivateKeyB().toString(16).substring(0, 32) + "... (128-bit)",
                "publicKeyB", dhResult.getPublicKeyB().toString(16).substring(0, 32) + "... (1024-bit)",
                "sharedSecret", dhResult.getSharedSecret().toString(16).substring(0, 32) + "... (shared)",
                "sharedSecretBitLength", dhResult.getSharedSecret().bitLength(),
                "sessionKeyAlgorithm", dhResult.getSessionKey().getAlgorithm(),
                "sessionKeyLength", dhResult.getSessionKey().getEncoded().length * 8
            ));
            
            System.out.println("=== TESTE DH CONCLUÍDO: " + (allTestsPassed ? "SUCESSO" : "FALHAS DETECTADAS") + " ===");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Erro no teste DH: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "TESTE FALHOU COM EXCEÇÃO!");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}