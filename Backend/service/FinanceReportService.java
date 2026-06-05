package com.hotelcraft.service;
 
import com.hotelcraft.model.Booking;
import com.hotelcraft.model.SupplyRequest;
import com.hotelcraft.model.Contract;
import com.hotelcraft.repository.BookingRepository;
import com.hotelcraft.repository.SupplyRequestRepository;
import com.hotelcraft.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
 
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
 
@Service
public class FinanceReportService {
 
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SupplyRequestRepository supplyRequestRepository;

    @Autowired
    private ContractRepository contractRepository;
 
    public Map<String, Object> getSummaryMetrics() {
        List<Booking> allActive = bookingRepository.findByStatusNot("Cancelled");
        
        double totalRevenue = allActive.stream().mapToDouble(Booking::getTotalAmount).sum();
        long totalBookings = allActive.size();
        double avgValue = totalBookings > 0 ? totalRevenue / totalBookings : 0.0;
 
        // Calculate growth (this month vs last month)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstOfThisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime firstOfLastMonth = firstOfThisMonth.minusMonths(1);
 
        double thisMonthRevenue = allActive.stream()
                .filter(b -> b.getCreatedAt().isAfter(firstOfThisMonth))
                .mapToDouble(Booking::getTotalAmount).sum();
 
        double lastMonthRevenue = allActive.stream()
                .filter(b -> b.getCreatedAt().isAfter(firstOfLastMonth) && b.getCreatedAt().isBefore(firstOfThisMonth))
                .mapToDouble(Booking::getTotalAmount).sum();
 
        double growth = 0.0;
        if (lastMonthRevenue > 0) {
            growth = ((thisMonthRevenue - lastMonthRevenue) / lastMonthRevenue) * 100;
        } else if (thisMonthRevenue > 0) {
            growth = 100.0;
        }
 
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalRevenue", totalRevenue);
        metrics.put("totalBookings", totalBookings);
        metrics.put("avgValue", avgValue);
        metrics.put("growth", growth);
        metrics.put("thisMonthRevenue", thisMonthRevenue);
        
        return metrics;
    }
 
    public Map<String, List<?>> getMonthlyRevenueData() {
        List<Booking> allActive = bookingRepository.findByStatusNot("Cancelled");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
 
        // Group by month
        Map<String, Double> monthlyMap = allActive.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getCreatedAt().format(formatter),
                        TreeMap::new, // Sorted keys might not work well with "Jun 2024" strings, but we can sort labels later
                        Collectors.summingDouble(Booking::getTotalAmount)
                ));
 
        // Ensure chronology (Last 6 months)
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
 
        LocalDateTime current = LocalDateTime.now().minusMonths(5);
        for (int i = 0; i < 6; i++) {
            String label = current.format(formatter);
            labels.add(label);
            data.add(monthlyMap.getOrDefault(label, 0.0));
            current = current.plusMonths(1);
        }
 
        Map<String, List<?>> result = new HashMap<>();
        result.put("labels", labels);
        result.put("data", data);
        return result;
    }
 
    public Map<String, Long> getStatusDistribution() {
        return bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(Booking::getStatus, Collectors.counting()));
    }
    
    public Map<String, Double> getPaymentMethodMethodDistribution() {
        List<Booking> allActive = bookingRepository.findByStatusNot("Cancelled");
        return allActive.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getPaymentMethod() != null ? b.getPaymentMethod() : "Unknown",
                        Collectors.summingDouble(Booking::getTotalAmount)
                ));
    }

    public Map<String, Object> getSupplierMetrics() {
        List<SupplyRequest> allApproved = supplyRequestRepository.findAll().stream()
                .filter(r -> "APPROVED".equals(r.getStatus()) || "RECEIVED".equals(r.getStatus()))
                .collect(Collectors.toList());

        double totalSpend = allApproved.stream()
                .mapToDouble(r -> (r.getApprovedQuantity() != null ? r.getApprovedQuantity() : r.getOfferedQuantity()) * r.getPricePerUnit())
                .sum();

        long activeContracts = contractRepository.findAll().stream()
                .filter(c -> "ACTIVE".equals(c.getStatus()))
                .count();

        double pendingProposalsValue = supplyRequestRepository.findByStatus("PENDING").stream()
                .mapToDouble(r -> r.getOfferedQuantity() * r.getPricePerUnit())
                .sum();

        Map<String, Object> supplierMetrics = new HashMap<>();
        supplierMetrics.put("totalSpend", totalSpend);
        supplierMetrics.put("activeContracts", activeContracts);
        supplierMetrics.put("pendingProposalsValue", pendingProposalsValue);
        supplierMetrics.put("receivedOrders", allApproved.stream().filter(r -> "RECEIVED".equals(r.getStatus())).count());

        return supplierMetrics;
    }
}
