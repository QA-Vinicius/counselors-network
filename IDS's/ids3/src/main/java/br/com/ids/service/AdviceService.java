package br.com.ids.service;

import br.com.ids.dto.ConselorsDTO;
import br.com.ids.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdviceService {

    @Autowired
    private JobScheduler jobScheduler;

    public void generatesAdvice(ConselorsDTO conselorsDTO) throws Exception {
        jobScheduler.processSample(conselorsDTO);
    }

    public void learnWithAdvice(ConselorsDTO conselorsDTO) throws Exception {
        jobScheduler.learnWithAdvice(conselorsDTO);
    }
}