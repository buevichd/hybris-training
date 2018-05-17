/*
 * [y] hybris Platform
 *
 * Copyright (c) 2017 SAP SE or an SAP affiliate company.  All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package com.expertsoft.merchandise.storefront.controllers.pages;

import com.expertsoft.merchandise.storefront.controllers.ControllerConstants;
import com.expertsoft.merchandise.storefront.forms.ElectronicsRegisterForm;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractRegisterPageController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.acceleratorstorefrontcommons.forms.ConsentForm;
import de.hybris.platform.acceleratorstorefrontcommons.forms.GuestForm;
import de.hybris.platform.acceleratorstorefrontcommons.forms.LoginForm;
import de.hybris.platform.acceleratorstorefrontcommons.forms.RegisterForm;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.AbstractPageModel;
import de.hybris.platform.commercefacades.user.data.RegisterData;
import de.hybris.platform.commerceservices.customer.DuplicateUidException;
import org.apache.log4j.Logger;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Register Controller for mobile. Handles login and register for the account flow.
 */
@Controller
@RequestMapping(value = "/register")
public class RegisterPageController extends AbstractRegisterPageController
{
	private HttpSessionRequestCache httpSessionRequestCache;

    private static final Logger LOGGER = Logger.getLogger(RegisterPageController.class);

    private static final String FORM_GLOBAL_ERROR = "form.global.error";

	@Override
	protected AbstractPageModel getCmsPage() throws CMSItemNotFoundException
	{
		return getContentPageForLabelOrId("register");
	}

	@Override
	protected String getSuccessRedirect(final HttpServletRequest request, final HttpServletResponse response)
	{
		if (httpSessionRequestCache.getRequest(request, response) != null)
		{
			return httpSessionRequestCache.getRequest(request, response).getRedirectUrl();
		}
		return "/";
	}

	@Override
	protected String getView()
	{
		return ControllerConstants.Views.Pages.Account.AccountRegisterPage;
	}

	@Resource(name = "httpSessionRequestCache")
	public void setHttpSessionRequestCache(final HttpSessionRequestCache accHttpSessionRequestCache)
	{
		this.httpSessionRequestCache = accHttpSessionRequestCache;
	}

	@RequestMapping(method = RequestMethod.GET)
	public String doRegister(final Model model) throws CMSItemNotFoundException
	{
		return getDefaultRegistrationPage(model);
	}

	@RequestMapping(value = "/newcustomer", method = RequestMethod.POST)
	public String doRegister(final ElectronicsRegisterForm form, final BindingResult bindingResult, final Model model,
			final HttpServletRequest request, final HttpServletResponse response, final RedirectAttributes redirectModel)
			throws CMSItemNotFoundException
	{
		getRegistrationValidator().validate(form, bindingResult);
		return processRegisterUserRequest(null, form, bindingResult, model, request, response, redirectModel);
	}

	@Override
	protected String getDefaultRegistrationPage(Model model) throws CMSItemNotFoundException {
		String result = super.getDefaultRegistrationPage(model);
		model.addAttribute("registerForm", new ElectronicsRegisterForm());
		return result;
	}

    @Override
    protected String processRegisterUserRequest(String referer, RegisterForm form, BindingResult bindingResult,
                                                Model model, HttpServletRequest request, HttpServletResponse response,
                                                RedirectAttributes redirectModel) throws CMSItemNotFoundException {
        ElectronicsRegisterForm registerForm = (ElectronicsRegisterForm) form;

        if (bindingResult.hasErrors())
        {
            return super.processRegisterUserRequest(referer, form, bindingResult, model, request, response, redirectModel);
        }

        final RegisterData data = new RegisterData();
        data.setFirstName(registerForm.getFirstName());
        data.setLastName(registerForm.getLastName());
        data.setLogin(registerForm.getEmail());
        data.setPassword(registerForm.getPwd());
        data.setTitleCode(registerForm.getTitleCode());
        data.setSecondEmail(registerForm.getSecondEmail());
        try
        {
            getCustomerFacade().register(data);
            getAutoLoginStrategy().login(registerForm.getEmail().toLowerCase(), registerForm.getPwd(), request, response);
            final ConsentForm consentForm = registerForm.getConsentForm();
            if (consentForm != null && consentForm.getConsentGiven())
            {
                getConsentFacade().giveConsent(consentForm.getConsentTemplateId(), consentForm.getConsentTemplateVersion());
            }
            GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
                    "registration.confirmation.message.title");
        }
        catch (final DuplicateUidException e)
        {
            LOGGER.warn("registration failed: ", e);
            model.addAttribute("registerForm", registerForm);
            model.addAttribute(new LoginForm());
            model.addAttribute(new GuestForm());
            bindingResult.rejectValue("email", "registration.error.account.exists.title");
            GlobalMessages.addErrorMessage(model, FORM_GLOBAL_ERROR);
            return handleRegistrationError(model);
        }

        return REDIRECT_PREFIX + getSuccessRedirect(request, response);
    }
}
