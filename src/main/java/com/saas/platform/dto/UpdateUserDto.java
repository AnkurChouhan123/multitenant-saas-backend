package com.saas.platform.dto;

public class UpdateUserDto {
    private String firstName;
    private String lastName;
    private String role;
    private Boolean active;
    
	public UpdateUserDto(String firstName, String lastName, String role, Boolean active) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
		this.role = role;
		this.active = active;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
    
	
    

}