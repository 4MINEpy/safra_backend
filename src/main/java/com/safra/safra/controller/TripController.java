package com.safra.safra.controller;

import com.safra.safra.dto.TripRequestDTO;
import com.safra.safra.entity.Trip;
import com.safra.safra.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class TripController {
    @Autowired
    TripService tripService;

    @RequestMapping(value = "/trips", method = RequestMethod.GET)
    public List<Trip> getAllTrips() {
        return tripService.getAllTrips();
    }
    @RequestMapping(value = "/trips/{id}", method = RequestMethod.GET)
    public Optional<Trip> getTrip(@PathVariable Long id) {
        return tripService.getTripById(id);
    }
    @RequestMapping(value = "/trips",method = RequestMethod.POST)
    public void createTrip(@RequestBody TripRequestDTO dto) {
        tripService.createTrip(dto);
    }
    @RequestMapping(value = "/trips/{id}",method = RequestMethod.DELETE)
    public void deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
    }
    @RequestMapping(value = "/trips",method = RequestMethod.PUT)
    public void updateTrip(@RequestBody Trip trip) {
        tripService.updateTrip(trip);
    }
}
