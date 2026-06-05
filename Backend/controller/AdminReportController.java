package com.hotelcraft.controller;
 
import com.hotelcraft.service.FinanceReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
 
@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasAuthority('REPORT_READ')")
public class AdminReportController {
 
    @Autowired
    private FinanceReportService financeReportService;
 
    @GetMapping
    public String viewReports(Model model) {
        model.addAttribute("metrics", financeReportService.getSummaryMetrics());
        model.addAttribute("monthlyRevenue", financeReportService.getMonthlyRevenueData());
        model.addAttribute("statusDistribution", financeReportService.getStatusDistribution());
        model.addAttribute("paymentMethods", financeReportService.getPaymentMethodMethodDistribution());
        model.addAttribute("supplierMetrics", financeReportService.getSupplierMetrics());
        model.addAttribute("activePage", "finance-reports");
        return "admin/reports";
    }
}
