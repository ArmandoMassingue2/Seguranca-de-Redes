package com.securemessaging.controller;

import com.securemessaging.exception.CertificateException;
import com.securemessaging.model.dto.CertificateDTO;
import com.securemessaging.model.entity.Certificate;
import com.securemessaging.model.entity.User;
import com.securemessaging.repository.CertificateRepository;
import com.securemessaging.service.UserService;
import com.securemessaging.service.crypto.PKIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct; // Correção: usar jakarta ao invés de javax
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pki")
public class PKIController {
    
    @Autowired
    private PKIService pkiService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CertificateRepository certificateRepository;
    
    // Inicializar CA Raiz na inicialização
    @PostConstruct
    public void initializePKI() {
        try {
            pkiService.initializeRootCA();
        } catch (CertificateException e) {
            System.err.println("Failed to initialize Root CA: " + e.getMessage());
        }
    }
    
    @GetMapping("/certificates")
    public String certificates(Principal principal, Model model) {
        Optional<User> userOpt = userService.findByUsername(principal.getName());
        if (userOpt.isPresent()) {
            List<Certificate> userCertificates = certificateRepository.findByOwner(userOpt.get());
            List<CertificateDTO> certificateDTOs = userCertificates.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            model.addAttribute("certificates", certificateDTOs);
            model.addAttribute("currentUser", principal.getName());
            
            // Adicionar informações da CA Raiz
            Certificate rootCA = pkiService.getRootCACertificate();
            if (rootCA != null) {
                model.addAttribute("rootCA", convertToDTO(rootCA));
            }
        }
        return "pki/certificates";
    }
    
    @GetMapping("/key-management")
    public String keyManagement(Principal principal, Model model) {
        model.addAttribute("currentUser", principal.getName());
        return "pki/key-management";
    }
    
    // API para gerar certificado assinado pela CA
    @PostMapping("/api/certificates/generate-ca-signed")
    @ResponseBody
    public ResponseEntity<?> generateCASignedCertificate(Principal principal) {
        try {
            Optional<User> userOpt = userService.findByUsername(principal.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            Certificate certificate = pkiService.generateCASignedCertificate(user);
            certificate = certificateRepository.save(certificate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("certificate", convertToDTO(certificate));
            response.put("message", "Certificado assinado pela CA gerado com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (CertificateException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/api/certificates/verify/{certificateId}")
    @ResponseBody
    public ResponseEntity<?> verifyCertificate(@PathVariable Long certificateId) {
        try {
            Optional<Certificate> certOpt = certificateRepository.findById(certificateId);
            if (certOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Certificate certificate = certOpt.get();
            boolean isValid = pkiService.verifyCertificate(certificate);
            boolean isSignedByCA = pkiService.isSignedByRootCA(certificate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("fingerprint", pkiService.getCertificateFingerprint(certificate));
            response.put("selfSigned", certificate.isSelfSigned());
            response.put("signedByCA", isSignedByCA);
            response.put("status", certificate.getStatus().toString());
            response.put("validFrom", certificate.getValidFrom().toString());
            response.put("validTo", certificate.getValidTo().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (CertificateException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/api/certificates/revoke/{certificateId}")
    @ResponseBody
    public ResponseEntity<?> revokeCertificate(@PathVariable Long certificateId, Principal principal) {
        try {
            Optional<Certificate> certOpt = certificateRepository.findById(certificateId);
            if (certOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Certificate certificate = certOpt.get();
            
            // Check if user owns the certificate
            if (!certificate.getOwner().getUsername().equals(principal.getName())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Unauthorized to revoke this certificate");
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            pkiService.revokeCertificate(certificate);
            certificateRepository.save(certificate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Certificado revogado com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // API para obter informações da CA Raiz
    @GetMapping("/api/ca/root")
    @ResponseBody
    public ResponseEntity<?> getRootCAInfo() {
        try {
            Certificate rootCA = pkiService.getRootCACertificate();
            if (rootCA == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Root CA not initialized");
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("subject", rootCA.getSubject());
            response.put("issuer", rootCA.getIssuer());
            response.put("serialNumber", rootCA.getSerialNumber());
            response.put("validFrom", rootCA.getValidFrom().toString());
            response.put("validTo", rootCA.getValidTo().toString());
            response.put("fingerprint", pkiService.getCertificateFingerprint(rootCA));
            response.put("selfSigned", rootCA.isSelfSigned());
            
            return ResponseEntity.ok(response);
            
        } catch (CertificateException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // API para validar cadeia de certificados
    @PostMapping("/api/certificates/validate-chain/{certificateId}")
    @ResponseBody
    public ResponseEntity<?> validateCertificateChain(@PathVariable Long certificateId) {
        try {
            Optional<Certificate> certOpt = certificateRepository.findById(certificateId);
            if (certOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Certificate certificate = certOpt.get();
            boolean isValid = pkiService.verifyCertificate(certificate);
            boolean isSignedByCA = pkiService.isSignedByRootCA(certificate);
            
            String trustLevel;
            if (certificate.isSelfSigned()) {
                trustLevel = "SELF_SIGNED";
            } else if (isSignedByCA) {
                trustLevel = "CA_SIGNED";
            } else {
                trustLevel = "UNKNOWN";
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("trustLevel", trustLevel);
            response.put("chainValid", isSignedByCA || certificate.isSelfSigned());
            response.put("certificate", convertToDTO(certificate));
            
            return ResponseEntity.ok(response);
            
        } catch (CertificateException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    private CertificateDTO convertToDTO(Certificate certificate) {
        CertificateDTO dto = new CertificateDTO();
        dto.setId(certificate.getId());
        if (certificate.getOwner() != null) {
            dto.setOwnerUsername(certificate.getOwner().getUsername());
        } else {
            dto.setOwnerUsername("ROOT_CA");
        }
        dto.setSerialNumber(certificate.getSerialNumber());
        dto.setSubject(certificate.getSubject());
        dto.setIssuer(certificate.getIssuer());
        dto.setValidFrom(certificate.getValidFrom());
        dto.setValidTo(certificate.getValidTo());
        dto.setStatus(certificate.getStatus());
        dto.setSelfSigned(certificate.isSelfSigned());
        
        try {
            dto.setFingerprint(pkiService.getCertificateFingerprint(certificate));
        } catch (CertificateException e) {
            dto.setFingerprint("Error generating fingerprint");
        }
        
        return dto;
    }
}