package com.carrer.ats_parser;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.apache.tika.Tika;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.io.ByteArrayOutputStream;

@RestController
public class HelloController {

    @GetMapping("/")
    public String showUploadPage() {
        return "<html>" +
                "<head><script src='https://cdn.tailwindcss.com'></script><title>Smart ATS Scorer</title></head>" +
                "<body class='bg-gray-50 min-h-screen flex flex-col items-center py-12 px-4 font-sans text-gray-800'>" +
                "   <div class='flex items-center gap-3 mb-10'><span class='text-4xl'>🚀</span><h1 class='text-3xl font-extrabold text-gray-900'>Smart ATS Resume Scorer</h1></div>" +
                "   <div class='bg-white p-8 rounded-2xl shadow-xl border border-gray-100 w-full max-w-3xl'>" +
                "       <form method='POST' action='/upload' enctype='multipart/form-data' class='space-y-8'>" +
                "           <div><label class='block text-sm font-semibold text-gray-700 mb-2 uppercase tracking-widest'>Step 1: Paste Job Description</label>" +
                "           <textarea name='jobDescription' rows='6' class='w-full p-4 border border-gray-200 rounded-xl outline-none bg-gray-50 focus:ring-2 focus:ring-green-500' placeholder='Paste job requirements here...'></textarea></div>" +
                "           <div><label class='block text-sm font-semibold text-gray-700 mb-2 uppercase tracking-widest'>Step 2: Upload Resume (PDF)</label>" +
                "           <div class='flex items-center justify-center w-full'><label class='flex flex-col items-center justify-center w-full h-32 border-2 border-gray-300 border-dashed rounded-xl cursor-pointer bg-gray-50 hover:bg-gray-100'>" +
                "           <p class='text-sm text-gray-500'><span class='font-semibold text-green-600'>Click to upload</span> or drag and drop</p><input type='file' name='file' class='hidden' accept='.pdf' /></label></div></div>" +
                "           <button type='submit' class='w-full bg-green-600 text-white font-bold py-4 rounded-xl shadow-lg hover:bg-green-700 transition-all active:scale-95'>Analyze & Calculate Match %</button>" +
                "       </form>" +
                "   </div>" +
                "   <p class='mt-8 text-gray-400 text-xs tracking-tighter'>Developed by DurgaPrasad | Java 21 & Spring Boot 4</p>" +
                "</body></html>";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("jobDescription") String jobDescription) {
        if (file.isEmpty()) return "Please select a file.";
        try {
            Tika tika = new Tika();
            String resumeText = tika.parseToString(file.getInputStream()).toLowerCase();
            String jobDescLower = jobDescription.toLowerCase();

            // Professional Technical Keywords
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
                        matchedHTML.append("<span class='px-3 py-1 bg-green-100 text-green-700 rounded-full text-xs font-bold mr-2'>✅ ").append(skill).append("</span> ");
                    } else {
                        missingHTML.append("<span class='px-3 py-1 bg-red-100 text-red-700 rounded-full text-xs font-bold mr-2'>❌ ").append(skill).append("</span> ");
                    }
                }
            }

            // Accurate Percentage Calculation
            int finalScore = (totalKeywordsInJD > 0) ? (matches * 100 / totalKeywordsInJD) : 0;

            return "<html><head><script src='https://cdn.tailwindcss.com'></script></head>" +
                    "<body class='bg-gray-50 min-h-screen flex flex-col items-center py-12 px-4 font-sans'>" +
                    "   <div class='bg-white p-8 rounded-2xl shadow-xl border border-gray-100 w-full max-w-2xl text-center'>" +
                    "       <h2 class='text-2xl font-bold mb-6 italic text-gray-700'>Results for: " + file.getOriginalFilename() + "</h2>" +
                    "       <div class='bg-green-50 p-6 rounded-xl mb-8'><p class='text-6xl font-black text-green-600'>" + finalScore + "%</p><p class='text-green-800 font-bold'>MATCH SCORE</p></div>" +
                    "       <div class='text-left space-y-6'>" +
                    "           <div><h3 class='text-xs font-bold text-gray-400 uppercase tracking-widest mb-2'>Detected Skills</h3>" +
                    "           <div class='flex flex-wrap gap-2'>" + (matchedHTML.length() > 0 ? matchedHTML : "None") + "</div></div>" +
                    "           <div><h3 class='text-xs font-bold text-gray-400 uppercase tracking-widest mb-2'>Skills to Improve</h3>" +
                    "           <div class='flex flex-wrap gap-2'>" + (missingHTML.length() > 0 ? missingHTML : "None") + "</div></div>" +
                    "       </div>" +
                    "       <div class='mt-10 flex justify-center gap-4'>" +
                    "           <a href='/' class='px-6 py-2 text-gray-500 hover:text-gray-800 font-semibold transition-colors'>← Back</a>" +
                    "           <a href='/download?score=" + finalScore + "&name=" + file.getOriginalFilename() + "' class='bg-blue-600 text-white px-8 py-2 rounded-lg font-bold hover:bg-blue-700 shadow-md transition-all'>Download PDF Report</a>" +
                    "       </div>" +
                    "   </div>" +
                    "</body></html>";
        } catch (Exception e) { return "<div class='p-10 text-red-600 font-bold'>Error during analysis: " + e.getMessage() + "</div>"; }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadReport(@RequestParam int score, @RequestParam String name) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Smart ATS Analysis Report").setBold().setFontSize(24));
            document.add(new Paragraph("--------------------------------------------------"));
            document.add(new Paragraph("Candidate Resume: " + name));
            document.add(new Paragraph("Job Match Score: " + score + "%"));
            document.add(new Paragraph("Status: " + (score >= 80 ? "High Match" : "Review Recommended")));
            document.add(new Paragraph("\nNote: This report was generated automatically based on keyword density analysis."));

            document.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ATS_Report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(out.toByteArray());
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }
}