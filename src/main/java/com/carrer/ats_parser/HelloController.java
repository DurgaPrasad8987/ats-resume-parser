package com.carrer.ats_parser;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.apache.tika.Tika;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HelloController {
    private final Tika tika = new Tika();

    @GetMapping("/")
    public String index() { return "index"; }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("jobDescription") String jd,
                                   Model model) {
        if (file.isEmpty()) {
            model.addAttribute("error", "Please select a file.");
            return "index";
        }

        try {
            String resumeText = tika.parseToString(file.getInputStream()).toLowerCase();
            String jdLower = jd.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", " ");

            // Comprehensive industry keyword list
            Set<String> masterKeywords = new HashSet<>(Arrays.asList(
                    "java", "spring", "sql", "maven", "rest", "api", "git", "docker",
                    "aws", "hibernate", "python", "artificial intelligence", "data science"
            ));

            // Only count keywords that actually appear in the Job Description
            List<String> requiredSkills = masterKeywords.stream()
                    .filter(jdLower::contains)
                    .collect(Collectors.toList());

            if (requiredSkills.isEmpty()) {
                model.addAttribute("score", 0);
                model.addAttribute("matched", "No matching requirements found in JD.");
                return "result";
            }

            List<String> matched = requiredSkills.stream()
                    .filter(resumeText::contains)
                    .collect(Collectors.toList());

            List<String> missing = requiredSkills.stream()
                    .filter(skill -> !resumeText.contains(skill))
                    .collect(Collectors.toList());

            // Accurate Score Calculation: (Matched / Required) * 100
            int score = (matched.size() * 100) / requiredSkills.size();

            model.addAttribute("score", score);
            model.addAttribute("fileName", file.getOriginalFilename());
            model.addAttribute("matched", String.join(", ", matched));
            model.addAttribute("missing", String.join(", ", missing));

            return "result";
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
            return "index";
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadReport(@RequestParam int score, @RequestParam String name) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.add(new Paragraph("Smart ATS Analysis Report").setBold().setFontSize(22));
            document.add(new Paragraph("Candidate: " + name));
            document.add(new Paragraph("Match Score: " + score + "%"));
            document.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ATS_Report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(out.toByteArray());
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }
}