/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.engine.app.api;

import io.bisq.common.UserThread;
import io.bisq.common.locale.Res;
import io.bisq.engine.app.util.Args;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import static org.springframework.util.MimeTypeUtils.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/preferences")
@Api(tags = {"Preferences"})
public class PreferencesApi extends ApiData {

    @RequestMapping(value = "/isTacAccepted", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Check if the user has accepted BISQ terms and conditions")
    public Message isTacAccepted(){
        Message message = new Message();
        if(preferences.isTacAccepted()){
            message.success = true;
            message.message = "BISQ TAC's have been accepted";
        }else{
            message.success = false;
            message.message = "BISQ TAC's have not been accepted";
            message.data = "1. In no event, unless for damages caused by acts of intent and gross negligence, damages resulting from personal injury, " +
                    "or damages ensuing from other instances where liability is required by applicable law or agreed to in writing, will any " +
                    "developer, copyright holder and/or any other party who modifies and/or conveys the software as permitted above or " +
                    "facilitates its operation, be liable for damages, including any general, special, incidental or consequential damages " +
                    "arising out of the use or inability to use the software (including but not limited to loss of data or data being " +
                    "rendered inaccurate or losses sustained by you or third parties or a failure of the software to operate with any " +
                    "other software), even if such developer, copyright holder and/or other party has been advised of the possibility of such damages.\n\n" +

                    "2. The user is responsible to use the software in compliance with local laws. Don't use the software if the usage is not legal in your jurisdiction.\n\n" +

                    "3. The " + Res.getBaseCurrencyName() + " market price is delivered by 3rd parties (BitcoinAverage, Poloniex, Coinmarketcap). " +
                    "It is your responsibility to verify the price with other sources for correctness.\n\n" +

                    "4. Any Fiat payment method carries a potential risk for bank chargeback. By accepting the \"User Agreement\" the users confirms " +
                    "to be aware of those risks and in no case will claim legal responsibility to the authors or copyright holders of the software.\n\n" +

                    "5. Any dispute, controversy or claim arising out of or relating to the use of the software shall be settled by arbitration in " +
                    "accordance with the Bisq arbitration rules as at present in force. The arbitration is conducted online. " +
                    "The language to be used in the arbitration proceedings shall be English if not otherwise stated.\n\n" +

                    "6. The user confirms that he has read and agreed to the rules regarding the dispute process:\n" +
                    "    - You must complete trades within the maximum duration specified for each payment method.\n" +
                    "    - You must enter the trade ID in the \"reason for payment\" text field when doing the fiat payment transfer.\n" +
                    "    - If the bank of the fiat sender charges fees the sender (" + Res.getBaseCurrencyCode() + " buyer) has to cover the fees.\n" +
                    "    - You must cooperate with the arbitrator during the arbitration process.\n" +
                    "    - You must reply within 48 hours to each arbitrator inquiry.\n" +
                    "    - Failure to follow the above requirements may result in loss of your security deposit.\n\n" +
                    "For more details and a general overview please read the full documentation about the " +
                    "arbitration system and the dispute process.";
        };
        return message;
    }

    @RequestMapping(value = "/setTacAccepted", method= RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Accept BISQ terms and conditions")
    public Message setTacAccepted() throws ExecutionException, InterruptedException {
        Message message = new Message();
        if(preferences.isTacAccepted()){
            message.success = false;
            message.message = "Terms and conditions are already accepted";
            return message;
        }
        if(Args.gui) {
            message.success = false;
            message.message = "Please accept the TAC from the GUI";
            return message;
        }
        CompletableFuture<Message> promise = new CompletableFuture<>();
        UserThread.execute(()->{
            preferences.setTacAccepted(true);
            rootView.checkIfLocalHostNodeIsRunning();
            message.success = true;
            message.message = "Terms and conditions have been accepted";
            promise.complete(message);
        });

        return promise.get();
    }
}
