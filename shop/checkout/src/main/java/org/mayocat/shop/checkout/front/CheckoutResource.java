/*
 * Copyright (c) 2012, Mayocat <hello@mayocat.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mayocat.shop.checkout.front;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.mayocat.rest.Resource;
import org.mayocat.rest.annotation.ExistingTenant;
import org.mayocat.shop.billing.model.Address;
import org.mayocat.shop.billing.model.Customer;
import org.mayocat.shop.cart.CartAccessor;
import org.mayocat.shop.cart.model.Cart;
import org.mayocat.shop.checkout.CheckoutException;
import org.mayocat.shop.checkout.CheckoutRegister;
import org.mayocat.shop.checkout.CheckoutResponse;
import org.mayocat.shop.front.views.WebView;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

/**
 * @version $Id$
 */
@Component(CheckoutResource.PATH)
@Path(CheckoutResource.PATH)
@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@ExistingTenant
public class CheckoutResource implements Resource
{
    public static final String PATH = "checkout";

    public static final String PAYMENT_RETURN_PATH = "return";

    public static final String PAYMENT_CANCEL_PATH = "cancel";

    @Inject
    private CheckoutRegister checkoutRegister;

    @Inject
    private CartAccessor cartAccessor;

    @Inject
    private Logger logger;

    private enum ErrorType
    {
        REQUIRED,
        BAD_VALUE;

        @JsonValue
        public String toJson()
        {
            return name().toLowerCase();
        }
    }

    private class Error
    {
        private String userMessage;

        private ErrorType errorType;

        public Error(ErrorType type, String userMessage)
        {
            this.errorType = type;
            this.userMessage = userMessage;
        }

        public String getUserMessage()
        {
            return userMessage;
        }

        public ErrorType getErrorType()
        {
            return errorType;
        }
    }

    @POST
    public Object checkout(@Context UriInfo uriInfo, MultivaluedMap data)
    {
        Map<String, Error> errors = Maps.newHashMap();
        String email = null;

        if (data.containsKey("email")) {
            email = (String) data.getFirst("email");
            EmailValidator emailValidator = EmailValidator.getInstance(false);
            if (!emailValidator.isValid(email)) {
                Error error = new Error(ErrorType.BAD_VALUE, "email is not valid");
                errors.put("email", error);
            }
        } else {
            Error error = new Error(ErrorType.REQUIRED, "email is mandatory");
            errors.put("email", error);
        }

        String firstName = getNonEmptyFieldValueOrAddToErrorMap("firstName", data, errors);
        String lastName = getNonEmptyFieldValueOrAddToErrorMap("lastName", data, errors);
        String street = getNonEmptyFieldValueOrAddToErrorMap("street", data, errors);
        String zip = getNonEmptyFieldValueOrAddToErrorMap("zip", data, errors);
        String city = getNonEmptyFieldValueOrAddToErrorMap("city", data, errors);
        String country = getNonEmptyFieldValueOrAddToErrorMap("country", data, errors);

        if (errors.keySet().size() > 0) {
            Map<String, Object> bindings = new HashMap<>();

            bindings.put("request", data);
            bindings.put("errors", errors);

            return new WebView().template("checkout/form.html").with(WebView.Option.FALLBACK_ON_DEFAULT_THEME)
                    .data(bindings);
        }

        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);

        Address deliveryAddress = new Address();
        deliveryAddress.setFullName(firstName + " " + lastName);
        deliveryAddress.setStreet(street);
        deliveryAddress.setZip(zip);
        deliveryAddress.setCity(city);
        deliveryAddress.setCountry(country);
        if (data.containsKey("company")) {
            String company = (String) data.getFirst("company");
            if (!Strings.isNullOrEmpty(company)) {
                deliveryAddress.setCompany(company);
            }
            String streetComplement = (String) data.getFirst("streetComplement");
            if (!Strings.isNullOrEmpty(streetComplement)) {
                deliveryAddress.setStreetComplement(streetComplement);
            }
        }

        try {
            Cart cart = cartAccessor.getCart();
            CheckoutResponse response = checkoutRegister.checkout(cart, uriInfo, customer, deliveryAddress, null);
            if (response.getRedirectURL().isPresent()) {
                return Response.seeOther(new URI(response.getRedirectURL().get())).build();
            } else {
                Map<String, Object> bindings = new HashMap<>();
                bindings.put("errors", errors);
                return new WebView().template("checkout/success.html").with(WebView.Option.FALLBACK_ON_DEFAULT_THEME)
                        .data(bindings);
            }
        } catch (final Exception e) {
            this.logger.error("Exception checking out", e);
            Map<String, Object> bindings = new HashMap<>();
            bindings.put("exception", new HashMap<String, Object>()
            {
                {
                    put("message", e.getMessage());
                }
            });
            return new WebView().template("checkout/exception.html").with(
                    WebView.Option.FALLBACK_ON_DEFAULT_THEME).data(bindings);
        }
    }

    @GET
    public Object checkout(@Context UriInfo uriInfo)
    {
        if (checkoutRegister.requiresForm()) {
            Map<String, Object> bindings = new HashMap<>();
            return new WebView().template("checkout/form.html").with(WebView.Option.FALLBACK_ON_DEFAULT_THEME)
                    .data(bindings);
        } else {
            try {
                Cart cart = cartAccessor.getCart();
                CheckoutResponse response = checkoutRegister.checkout(cart, uriInfo, null, null, null);

                if (response.getRedirectURL().isPresent()) {
                    return Response.seeOther(new URI(response.getRedirectURL().get())).build();
                }
            } catch (final CheckoutException e) {
                Map<String, Object> bindings = new HashMap<>();
                bindings.put("exception", new HashMap<String, Object>()
                {
                    {
                        put("message", e.getMessage());
                    }
                });
                return new WebView().template("checkout/exception.html")
                        .with(WebView.Option.FALLBACK_ON_DEFAULT_THEME).data(bindings);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return Response.ok().build();
    }

    @GET
    @Path(PAYMENT_RETURN_PATH)
    public WebView returnFromExternalPaymentService(@Context UriInfo uriInfo)
    {
        for (String key : uriInfo.getQueryParameters().keySet()) {
            System.out.println(key + " : " + uriInfo.getQueryParameters().getFirst(key));
        }
        Map<String, Object> bindings = new HashMap<>();
        return new WebView().template("checkout/success.html").with(WebView.Option.FALLBACK_ON_DEFAULT_THEME)
                .data(bindings);
    }

    @GET
    @Path("{orderId}/" + PAYMENT_CANCEL_PATH)
    public WebView cancelFromExternalPaymentService(@PathParam("orderId") UUID orderId)
    {
        try {
            checkoutRegister.dropOrder(orderId);
        } catch (CheckoutException e) {
            this.logger.error("Failed to cancel order", e);
        }
        Map<String, Object> bindings = new HashMap<>();
        return new WebView().template("checkout/cancelled.html").with(WebView.Option.FALLBACK_ON_DEFAULT_THEME)
                .data(bindings);
    }

    private String getNonEmptyFieldValueOrAddToErrorMap(String field, MultivaluedMap data, Map<String, Error> errors)
    {
        if (!data.containsKey(field) || Strings.isNullOrEmpty((String) data.getFirst(field))) {
            Error error = new Error(ErrorType.REQUIRED, StringUtils.capitalize(field) + " is mandatory");
            errors.put(field, error);
            return null;
        } else {
            return (String) data.getFirst(field);
        }
    }
}
