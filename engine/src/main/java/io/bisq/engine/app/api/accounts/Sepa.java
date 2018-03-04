package io.bisq.engine.app.api.accounts;

import io.bisq.business.formatters.Message;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import static io.bisq.business.Handlers.checkErrors;
import static io.bisq.business.actions.accounttypes.Sepa.newSepa;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/account/create")
@Api(tags = {"Account"})
public class Sepa {

    @RequestMapping(value = "/SEPA", method = RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a new SEPA payment account")
    public Message createSEPA(

            @ApiParam(value = "Account owner full name", required=true)
            @RequestParam(value = "holderName")
                    String holderName,

            @ApiParam(value = "IBAN code", required=true)
            @RequestParam(value = "iban")
                    String iban,

            @ApiParam(value = "BIC code", required=true)
            @RequestParam(value = "bic")
                    String bic,

            @ApiParam(value = "Country code of bank (eg. \"IE\" for Ireland)", required=true)
            @RequestParam(value = "countryCode")
                    String countryCode,

            @ApiParam(value = "Trading currency code (eg. \"EUR\" for Euro)", required=true)
            @RequestParam(value = "currencyCode")
                    String currencyCode

    ) throws Exception {
        checkErrors();
        return newSepa(holderName,iban,bic,countryCode,currencyCode);
    }
}
