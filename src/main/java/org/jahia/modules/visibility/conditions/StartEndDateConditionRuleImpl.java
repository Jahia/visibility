package org.jahia.modules.visibility.conditions;

import java.util.Calendar;
import java.util.Locale;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.visibility.BaseVisibilityConditionRule;
import org.jahia.utils.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handle the execution of a start/end date condition for checking a node visibility.
 * If the current date is after the start date or if start date is not defined then the node is visible unless
 * end date is defined and we are after end date.
 *
 * @author rincevent
 * @since JAHIA 6.6
 * Created : 8/29/11
 */
public class StartEndDateConditionRuleImpl extends BaseVisibilityConditionRule {

    private transient static Logger LOGGER = LoggerFactory.getLogger(StartEndDateConditionRuleImpl.class);

    /**
     * Return the associated display template that will be used by gwt.
     *
     * @return Return the associated display template that will be used by gwt.
     */
    public String getGWTDisplayTemplate(Locale locale) {
        return Messages.get(ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackage("Jahia Visibility"), "label.startEndDateCondition.xtemplate", locale);
    }

    public boolean matches(JCRNodeWrapper nodeWrapper) {
        Calendar start = null;
        Calendar end = null;
        try {
            start = nodeWrapper.getProperty("start").getValue().getDate();
        } catch (PathNotFoundException e) {
            LOGGER.debug("start is not defined for this rule");
        } catch (RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
        }
        try {
            end = nodeWrapper.getProperty("end").getValue().getDate();
        } catch (PathNotFoundException e) {
            LOGGER.debug("end is not defined for this rule");
        } catch (RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
        }
        Calendar calendar = null;
        try {
            calendar = nodeWrapper.getSession().getPreviewDate();
        } catch (RepositoryException e) {
        }
        if (calendar == null) {
            calendar = Calendar.getInstance();
        }
        if (start != null) {
            if (!calendar.after(start)) {
                return false;
            }
        }
        if (end != null) {
            if (!calendar.before(end)) {
                return false;
            }
        }
        return true;
    }

}
