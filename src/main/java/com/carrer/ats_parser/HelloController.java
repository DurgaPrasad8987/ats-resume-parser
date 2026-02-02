package com.carrer.ats_parser;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.Tika;

@RestController
public class HelloController {

    // 1. THIS WAS MISSING: The code to actually show the website
    @GetMapping("/")
    public String showUploadPage() {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif; margin: 40px; line-height: 1.6;'>" +
                "<h2>🚀 ATS Resume Parser & Scorer</h2>" +
                "<form method='POST' action='/upload' enctype='multipart/form-data' style='background: #f4f4f4; padding: 20px; border-radius: 8px;'>" +
                "   <p><strong>Step 1: Paste Job Description</strong></p>" +
                "   <textarea name='jobDescription' rows='6' style='width: 100%;' placeholder='Paste the job requirements here...'></textarea><br><br>" +
                "   <p><strong>Step 2: Upload your Resume (PDF)</strong></p>" +
                "   <input type='file' name='file' /><br><br>" +
                "   <button type='submit' style='background: #28a745; color: white; padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer;'>Analyze Resume</button>" +
                "</form>" +
                "</body>" +
                "</html>";
    }

    // 2. The logic to process the upload
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("jobDescription") String jobDescription) {
        if (file.isEmpty()) return "Please select a file.";

        try {
            Tika tika = new Tika();
            String resumeText = tika.parseToString(file.getInputStream()).toLowerCase();
            String jobDescLower = jobDescription.toLowerCase();

            String[] keywords = {"java", "spring", "sql", "maven", "rest", "api", "git", "docker", "aws"};
            int score = 0;
            StringBuilder matched = new StringBuilder();
            StringBuilder missing = new StringBuilder();

            for (String skill : keywords) {
                if (jobDescLower.contains(skill)) {
                    if (resumeText.contains(skill)) {
                        score += 15;
                        matched.append("<span style='color: green;'>✅ ").append(skill).append("</span> ");
                    } else {
                        missing.append("<span style='color: red;'>❌ ").append(skill).append("</span> ");
                    }
                }
            }

            return "<html><body style='font-family: Arial; margin: 40px;'>" +
                    "<h2>Analysis Results for: " + file.getOriginalFilename() + "</h2>" +
                    "<div style='background: #f8f9fa; padding: 20px; border-radius: 10px; border-left: 5px solid #28a745;'>" +
                    "   <p style='font-size: 20px;'><strong>Job Match Score: " + score + "%</strong></p>" +
                    "   <p><strong>Skills You Have:</strong> " + (matched.length() > 0 ? matched : "None identified") + "</p>" +
                    "   <p><strong>Skills to Add for a Better Score:</strong> " + (missing.length() > 0 ? missing : "None! You are a perfect match.") + "</p>" +
                    "</div>" +
                    "<br><a href='/'>← Upload Another Resume</a>" +
                    "</body></html>";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}