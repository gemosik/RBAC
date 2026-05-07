package com.example.taxi.trip.service;

public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
