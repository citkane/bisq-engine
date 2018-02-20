/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.bisq.engine.app.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import static org.springframework.util.MimeTypeUtils.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferences")
@Api(tags = {"Preferences"})
public class PreferencesApi extends ApiData {

    @RequestMapping(value = "/isTacAccepted", method= RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Check if the user has accepted BISQ terms and conditions")
    public Boolean isTacAccepted(){
        return preferences.isTacAccepted();
    }

    @RequestMapping(value = "/setTacAccepted", method= RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Accept BISQ terms and conditions")
    public Message setTacAccepted(){
        Message message = new Message();
        if(preferences.isTacAccepted()){
            message.message = "Terms and conditions are already accepted";
            return message;
        }
        preferences.setTacAccepted(true);
        rootView.checkIfLocalHostNodeIsRunning();
        message.message = "Terms and conditions have been accepted";
        return message;
    }
}
