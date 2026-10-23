package com.tpobuddy.controller;

import com.tpobuddy.model.ResumeData;
import com.tpobuddy.model.User;
import com.tpobuddy.service.FirebaseService;
import com.tpobuddy.service.ResumeAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Controller for Resume upload, Apache PDFBox text extraction, and ATS scoring.
 */
@Controller
public class ResumeController {

    private final FirebaseService firebaseService;

    public ResumeController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @GetMapping({"/resume", "/resume-analysis.jsp"})
    public String viewResume(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return "redirect:/login.jsp";
        }

        ResumeData resume = firebaseService.fetchResumeData(user.getUid());
        model.addAttribute("resume", resume);
        model.addAttribute("user", user);
        return "resume-analysis";
    }

    @PostMapping({"/resume", "/api/resume/upload"})
    public String uploadResume(
            @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile,
            @RequestParam(value = "resumeText", required = false) String resumeText,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "projects", required = false) String projects,
            HttpServletRequest request,
            Model model) {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (user == null) {
            return "redirect:/login.jsp";
        }

        String uid = user.getUid();

        // Extract text from uploaded PDF if provided
        if (resumeFile != null && !resumeFile.isEmpty()) {
            try (InputStream in = resumeFile.getInputStream()) {
                String extracted = ResumeAnalysisService.extractTextFromPdf(in);
                if (extracted != null && !extracted.trim().isEmpty()) {
                    resumeText = extracted;
                }
            } catch (Exception e) {
                System.err.println("Resume file extraction error: " + e.getMessage());
            }
        }

        if (resumeText == null || resumeText.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Please upload a valid PDF file or paste your resume text.");
            return viewResume(request, model);
        }

        // Analyze resume text and calculate ATS metrics
        ResumeData analysis = ResumeAnalysisService.analyzeResumeText(uid, resumeText);
        if (experience != null) analysis.setExperience(experience);
        if (projects != null) analysis.setProjects(projects);

        // Persist to Firebase
        firebaseService.saveResumeData(analysis);

        model.addAttribute("resume", analysis);
        model.addAttribute("successMessage", "Resume analyzed and saved to Firebase successfully! ATS score updated.");
        model.addAttribute("user", user);
        return "resume-analysis";
    }
}
