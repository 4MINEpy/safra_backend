package com.safra.safra.controller;

import com.safra.safra.dto.TripRequestDTO;
import com.safra.safra.entity.Trip;
import com.safra.safra.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class TripController {


}