package com.safra.safra.service;


import com.safra.safra.entity.Car;
import com.safra.safra.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CarService {

    @Autowired
    private final CarRepository carRepository;

    public List<Car> getCars() {return carRepository.findAll();}
    public Optional<Car> getCarById(Long id) {return carRepository.findById(id);}
    public void createCar(Car car) {carRepository.save(car);}
    public void deleteCar(Long id) {carRepository.deleteById(id);}
    public void updateCar(Car car) {carRepository.save(car);}



}
