/* Path: src/main/java/com/carrer/ats_parser/HelloController.java */
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
import java.util.ArrayList;
import java.util.Arrays;

@Controller
public class HelloController {
    private final Tika tika = new Tika();

    @GetMapping("/")
    public String showUploadPage() { return "index"; }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("jobDescription") String jobDescription,
                                   Model model) {
        if (file.isEmpty()) {
            model.addAttribute("error", "Please select a file.");
            return "index";
        }

        try {
            String resumeText = tika.parseToString(file.getInputStream()).toLowerCase();
            String jobDescLower = jobDescription.toLowerCase();

            String[] keywords = {"java", "spring", "sql", "maven", "rest", "api", "git", "docker", "aws", "hibernate", "python", "artificial intelligence"};

            int totalKeywordsInJD = 0;
            int matches = 0;

            // FIX: Using java.util.List specifically to avoid the iText error
            java.util.List<String> matchedSkills = new java.util.ArrayList<>();
            java.util.List<String> missingSkills = new java.util.ArrayList<>();

            for (String skill : keywords) {
                if (jobDescLower.contains(skill)) {
                    totalKeywordsInJD++;
                    if (resumeText.contains(skill)) {
                        matches++;
                        matchedSkills.add(skill);
                    } else {
                        missingSkills.add(skill);
                    }
                }
            }

            int finalScore = (totalKeywordsInJD > 0) ? (matches * 100 / totalKeywordsInJD) : 0;

            model.addAttribute("score", finalScore);
            model.addAttribute("fileName", file.getOriginalFilename());
            model.addAttribute("matched", String.join(", ", matchedSkills));
            model.addAttribute("missing", String.join(", ", missingSkills));

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