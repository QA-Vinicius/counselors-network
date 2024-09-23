package br.com.ids.controller;

import br.com.ids.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("api/cnw")
public class StartProcessController {

    @Autowired
    private JobScheduler jobScheduler;

    @PostMapping("/start")
    public ResponseEntity<String> startTrainingProcess() {
        try {
            jobScheduler.startProcess();
            return ResponseEntity.ok("Successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Failed to start execution: " + e.getMessage());
        }
    }

}
