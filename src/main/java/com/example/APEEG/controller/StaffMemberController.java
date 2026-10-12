package com.example.APEEG.controller;

import com.example.APEEG.model.StaffMember;
import com.example.APEEG.service.StaffMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff-members")
@CrossOrigin(origins = "*")
public class StaffMemberController {

    private final StaffMemberService staffMemberService;

    public StaffMemberController(StaffMemberService staffMemberService) {
        this.staffMemberService = staffMemberService;
    }

    @GetMapping
    public ResponseEntity<?> getMyStaffMembers() {
        try {
            List<StaffMember> staffList = staffMemberService.getMyStaffMembers();
            return ResponseEntity.ok(staffList);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addStaffMember(@RequestBody StaffMember staffMember) {
        try {
            StaffMember saved = staffMemberService.saveOrGetStaffMember(
                    staffMember.getName(),
                    staffMember.getEmail(),
                    staffMember.getMobile(),
                    staffMember.getDepartment()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}