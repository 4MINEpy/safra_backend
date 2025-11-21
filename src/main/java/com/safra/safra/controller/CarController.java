package com.safra.safra.controller;


import com.safra.safra.entity.Car;
import com.safra.safra.service.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class CarController {
    @Autowired
    private CarService carService;

    @RequestMapping(value = "/cars",method = RequestMethod.GET)
    public List<Car> getCars() {
        return carService.getCars();
    }
    @RequestMapping(value = "/cars/{id}",method = RequestMethod.GET)
    public Optional<Car> getCar(@PathVariable long id) {
        return carService.getCarById(id);
    }

    @RequestMapping(value = "/cars",method = RequestMethod.POST)
    public void createCar(@RequestBody Car car) {
        carService.createCar(car);
    }
    @RequestMapping(value = "/cars",method = RequestMethod.PUT)
    public void updateCar(@RequestBody Car car) {
        carService.updateCar(car);
    }
    @RequestMapping(value = "/cars/{id}",method = RequestMethod.DELETE)
    public void deleteCar(@PathVariable Long id) {
        carService.deleteCar(id);
    }
}
