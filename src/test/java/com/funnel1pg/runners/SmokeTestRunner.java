package com.funnel1pg.runners;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:reports/cucumber-report.html, json:reports/cucumber-report.json, junit:reports/cucumber-report.xml")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME,
        value = "com.funnel1pg.stepdefs, com.funnel1pg.hooks")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@smoke")
public class SmokeTestRunner {
}
