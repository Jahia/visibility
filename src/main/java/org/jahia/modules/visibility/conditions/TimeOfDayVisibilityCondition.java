/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.visibility.conditions;

import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.visibility.BaseVisibilityConditionRule;
import org.jahia.utils.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.Calendar;
import java.util.Locale;

/**
 * Defines the visibility condition, based on the time of the day. The content is considered visible if the current time of the day is
 * between the start and end time. If one of them is not defined, the other is considered only.
 *
 * @author Sergiy Shyrkov
 */
public class TimeOfDayVisibilityCondition extends BaseVisibilityConditionRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeOfDayVisibilityCondition.class);

    public String getGWTDisplayTemplate(Locale locale) {
        return Messages.get(ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackage("Jahia Visibility"), "label.timeOfDayCondition.xtemplate", locale);
    }

    protected Integer getValue(JCRNodeWrapper node, String propertyName, Integer defaultValue) throws RepositoryException {
        Integer intValue = defaultValue;
        try {
            String val = node.getProperty(propertyName).getString();
            if (val != null) {
                intValue = Integer.parseInt(val);
            }
        } catch (PathNotFoundException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} is not defined for this condition on the node {}", propertyName, node.getPath());
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Error parsing the value of the property {} for this condition on the node {}. Cause: {}", propertyName, node.getPath(), e.getMessage());
        }

        return intValue;
    }

    public boolean matches(JCRNodeWrapper node) {
        boolean visible;
        Integer startHour = 0;
        Integer startMinute = 0;
        Integer endHour = 23;
        Integer endMinute = 59;

        try {
            startHour = getValue(node, "startHour", 0);
            startMinute = getValue(node, "startMinute", 0);
            endHour = getValue(node, "endHour", 23);
            endMinute = getValue(node, "endMinute", 59);
        } catch (RepositoryException e) {
            LOGGER.error("Error reading time settings for the visibility condition node {} Cause: {}",node.getPath(), e.getMessage(), e);
        }

        Calendar now = null;
        try {
            now = node.getSession().getPreviewDate();
        } catch (RepositoryException e) {  // ignore it
        }
        if (now == null) {
            now = Calendar.getInstance();
        }

        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        visible = (startHour < hour || (startHour == hour && startMinute <= minute))
                && (endHour > hour || (endHour == hour && endMinute > minute));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Time of the day condition with time '{}:{}-{}:{}' on node {} evaluated to {}", startHour, startMinute, endHour, endMinute, node.getPath(), visible);
        }

        return visible;
    }
}
