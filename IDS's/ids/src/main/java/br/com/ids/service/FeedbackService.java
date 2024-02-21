package br.com.ids.service;

import br.com.ids.dto.ConselorsDTO;

public class FeedbackService {
    public void learnAndRetrain(ConselorsDTO value) {
        /* Logica adotada pelo conselheiro que solicitou o conselho.
        *
        * Com o conselho recebido, ele ira aprender e retreinar
        * sua base de conhecimento
        */
    }

    public void learnWithFeedback(ConselorsDTO value) {
        /* Logica adotada pelos outros conselheiros da rede
        *
        * Mesmo sem solicitar conselho, irao escutar e
        * aprender para aprimorar sua base de conhecimento
        */
    }
}
