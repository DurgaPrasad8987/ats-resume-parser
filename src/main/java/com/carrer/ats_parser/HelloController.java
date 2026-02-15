package com.carrer.ats_parser;

import org.springframework.stereotype.Controller; // Changed from @RestController
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

@Controller // Changed to @Controller to support HTML templates
public class HelloController {

    @GetMapping("/")
    public String showUploadPage() {
        // This now looks for src/main/resources/templates/index.html
        return "index";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("jobDescription") String jobDescription,
                                   Model model) {
        if (file.isEmpty()) {
            model.addAttribute("error", "Please select a file.");
            return "index";
        }

        try {
            Tika tika = new Tika();
            String resumeText = tika.parseToString(file.getInputStream()).toLowerCase();
            String jobDescLower = jobDescription.toLowerCase();

            String[] keywords = {"java", "spring", "sql", "maven", "rest", "api", "git", "docker", "aws"};

            int totalKeywordsInJD = 0;
            int matches = 0;
            StringBuilder matchedHTML = new StringBuilder();
            StringBuilder missingHTML = new StringBuilder();

            for (String skill : keywords) {
                if (jobDescLower.contains(skill)) {
                    totalKeywordsInJD++;
                    if (resumeText.contains(skill)) {
                        matches++;
                        matchedHTML.append(skill).append(", ");
                    } else {
                        missingHTML.append(skill).append(", ");
                    }
                }
            }

            int finalScore = (totalKeywordsInJD > 0) ? (matches * 100 / totalKeywordsInJD) : 0;

            // Adding data to the Model to show in your result.html template
            model.addAttribute("score", finalScore);
            model.addAttribute("fileName", file.getOriginalFilename());
            model.addAttribute("matched", matchedHTML.toString());
            model.addAttribute("missing", missingHTML.toString());

            return "result"; // This will look for src/main/resources/templates/result.html

        } catch (Exception e) {
            model.addAttribute("error", "Error during analysis: " + e.getMessage());
            return "index";
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadReport(@RequestParam int score, @RequestParam String name) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Smart ATS Analysis Report").setBold().setFontSize(24));
            document.add(new Paragraph("Candidate Resume: " + name));
            document.add(new Paragraph("Job Match Score: " + score + "%"));
            document.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ATS_Report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(out.toByteArray());
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }
}