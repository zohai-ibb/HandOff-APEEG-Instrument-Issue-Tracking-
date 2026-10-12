package com.example.APEEG.service;

import com.example.APEEG.model.Person;
import com.example.APEEG.model.StaffMember;
import com.example.APEEG.repository.StaffMemberRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StaffMemberService {

    private final StaffMemberRepository staffMemberRepository;

    public StaffMemberService(StaffMemberRepository staffMemberRepository) {
        this.staffMemberRepository = staffMemberRepository;
    }

    /**
     * Retrieves or creates a staff member for the authenticated scientist.
     * If the staff member exists (matched by email under this scientist), returns it.
     * Otherwise, persists a new StaffMember entity.
     */
    public StaffMember saveOrGetStaffMember(String name, String email, String mobile, String department) {
        Person authenticatedScientist = getAuthenticatedScientist();
        if (authenticatedScientist == null) {
            throw new SecurityException("Unauthorized: Valid scientist authentication required.");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Staff name is required.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Staff email is required.");
        }

        String cleanEmail = email.trim().toLowerCase();

        // Check if staff member already exists for this scientist
        Optional<StaffMember> existingStaff = staffMemberRepository
                .findByOwnerScientistIdAndEmail(authenticatedScientist.getId(), cleanEmail);

        if (existingStaff.isPresent()) {
            return existingStaff.get();
        }

        // Save new staff member under the authenticated scientist
        StaffMember newStaff = new StaffMember();
        newStaff.setName(name.trim());
        newStaff.setEmail(cleanEmail);
        newStaff.setMobile(mobile != null ? mobile.trim() : null);
        newStaff.setDepartment(department != null ? department.trim() : "APEEG");
        newStaff.setOwnerScientist(authenticatedScientist);

        return staffMemberRepository.save(newStaff);
    }

    /**
     * Gets all saved staff members for the authenticated scientist.
     */
    public List<StaffMember> getMyStaffMembers() {
        Person authenticatedScientist = getAuthenticatedScientist();
        if (authenticatedScientist == null) {
            throw new SecurityException("Unauthorized: Valid scientist authentication required.");
        }
        return staffMemberRepository.findByOwnerScientistId(authenticatedScientist.getId());
    }

    private Person getAuthenticatedScientist() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Person) {
            return (Person) principal;
        }
        return null;
    }
}