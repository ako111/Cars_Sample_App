/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.supercars.externaldata;

import brave.Tracing;
import brave.jaxrs2.TracingClientFilter;
import com.supercars.Car;
import com.supercars.Manufacturer;
import com.supercars.Rating;
import com.supercars.RatingRequest;
import com.supercars.dataloader.CarDataLoader;
import com.supercars.preferences.Preference;
import com.supercars.preferences.PreferenceManager;
import com.supercars.tracing.TracingHelper;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import com.supercars.preferences.PreferenceException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author tombatchelor
 */
public class CarRating {

    static Tracing tracing = TracingHelper.getTracing(TracingHelper.RATING_NAME);

    private final static Logger logger = Logger.getLogger(CarRating.class.getName());
    
    public static Rating getCarRating(int carID) {
        logger.log(Level.FINE, "Getting rating for carID: {0}", carID);
        Car car = new CarDataLoader().getCar(carID);
        Manufacturer manufacturer = car.getManufacturer();
        RatingRequest ratingRequest = new RatingRequest(manufacturer.getName(), car.getModel());
        try {
            Preference preference = PreferenceManager.getPreference("REST_CLIENT");
            switch (preference.getValue()) {
                case "Jersey_Sync":
                    return getCarRatingSync(ratingRequest);
                case "Jersey_Async":
                    return getCarRatingAsync(ratingRequest).get();
            }
            logger.log(Level.FINE, "Success getting insurance quote for carID: {0}", carID);
        } catch (InterruptedException | PreferenceException | ExecutionException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        return null;
    }

    private static Rating getCarRatingSync(RatingRequest ratingRequest) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("https://qsii07giue.execute-api.us-west-2.amazonaws.com/test/carrating");
        target.register(TracingClientFilter.create(tracing));
        return target.request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(ratingRequest, MediaType.APPLICATION_JSON), Rating.class);
    }

    private static Future<Rating> getCarRatingAsync(RatingRequest ratingRequest) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("https://qsii07giue.execute-api.us-west-2.amazonaws.com/test/carrating");
        Future<Rating> response = target.request(MediaType.APPLICATION_JSON).async()
                .post(Entity.entity(ratingRequest, MediaType.APPLICATION_JSON), Rating.class);
        return response;
    }
}