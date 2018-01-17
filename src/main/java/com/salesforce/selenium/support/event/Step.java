/* 
 * Copyright (c) 2018, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license. 
 * For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.selenium.support.event;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Records information on a given WebDriver command such as click() or getText().
 * 
 * The {@link EventFiringWebDriver} creates such a record before and after each command. The Step object
 * is then passed on to any listeners implementing the {@link WebDriverEventListener} interface.
 * 
 * A special listener is {@link FullJSONLogger} which collects all Step objects and saves the collection
 * to a JSON file.
 * 
 * @author gneumann
 * @since 2.0.0
 */
public class Step {
	public enum Type { BeforeAction, AfterAction, BeforeGather, AfterGather, Exception }
	public enum Cmd {
		// commands called directly from WebDriver object
		close, findElementByWebDriver, findElementsByWebDriver, get, getCurrentUrl, getTitle, getWindowHandle, getWindowHandles, quit,
		// commands called directly from WebDriver.Navigation object
		back, forward, refresh, to,
		// commands called directly from WebDriver.TargetLocator object
		activeElement, alert, defaultContent, frameByIndex, frameByName, frameByElement, parentFrame, window,
		// commands called directly from WebDriver.Window object
		fullscreen, getPosition, getSize, maximize, setPosition, setSize,
		// commands called directly from WebElement object
		click, clear, findElementByElement, findElementsByElement, getAttribute, getCssValue, getTagName, getText, isDisplayed, isEnabled, isSelected, sendKeys, submit,
		// current command has failed 
		testFailure;
		
		@Override
		public String toString() {
			String value = null;
			switch (this) {
			case close:
				value = "webDriver.close";
				break;
			case findElementByWebDriver:
				value = "webDriver.findElement";
				break;
			case findElementsByWebDriver:
				value = "webDriver.findElements";
				break;
			case get:
				value = "webDriver.get";
				break;
			case getCurrentUrl:
				value = "webDriver.getCurrentUrl";
				break;
			case getTitle:
				value = "webDriver.getTitle";
				break;
			case getWindowHandle:
				value = "webDriver.getWindowHandle";
				break;
			case getWindowHandles:
				value = "webDriver.getWindowHandles";
				break;
			case quit:
				value = "webDriver.quit";
				break;
			case back:
				value = "webDriver.navigate().back";
				break;
			case forward:
				value = "webDriver.navigate().forward";
				break;
			case refresh:
				value = "webDriver.navigate().refresh";
				break;
			case to:
				value = "webDriver.navigate().to";
				break;
			case activeElement:
				value = "webDriver.switchTo().activeElement";
				break;
			case alert:
				value = "webDriver.switchTo().alert";
				break;
			case defaultContent:
				value = "webDriver.switchTo().defaultContent";
				break;
			case frameByIndex:
				value = "webDriver.switchTo().frame";
				break;
			case frameByName:
				value = "webDriver.switchTo().frame";
				break;
			case frameByElement:
				value = "webDriver.switchTo().frame";
				break;
			case parentFrame:
				value = "webDriver.switchTo().parentFrame";
				break;
			case window:
				value = "webDriver.switchTo().window";
				break;
			case fullscreen:
				value = "webDriver.manage().window().fullscreen";
				break;
			case getPosition:
				value = "webDriver.manage().window().getPosition";
				break;
			case getSize:
				value = "webDriver.manage().window().getSize";
				break;
			case maximize:
				value = "webDriver.manage().window().maximize";
				break;
			case setPosition:
				value = "webDriver.manage().window().setPosition";
				break;
			case setSize:
				value = "webDriver.manage().window().setSize";
				break;
			case click:
				value = "webElement.click";
				break;
			case clear:
				value = "webElement.clear";
				break;
			case findElementByElement:
				value = "webElement.findElement";
				break;
			case findElementsByElement:
				value = "webElement.findElements";
				break;
			case getAttribute:
				value = "webElement.getAttribute";
				break;
			case getCssValue:
				value = "webElement.getCssValue";
				break;
			case getTagName:
				value = "webElement.getTagName";
				break;
			case getText:
				value = "webElement.getText";
				break;
			case isDisplayed:
				value = "webElement.isDisplayed";
				break;
			case isEnabled:
				value = "webElement.isEnabled";
				break;
			case isSelected:
				value = "webElement.isSelected";
				break;
			case sendKeys:
				value = "webElement.sendKeys";
				break;
			case submit:
				value = "webElement.submit";
				break;
			default:
				value = "unknown";
			}
			return value;
		}
	}

	private static long timeMarkerElapsedStep;
	private static long timeMarkerSinceLastStep;
	private static int lastRecordNumber = 1;

	private int recordNumber = -1;
	private int stepNumber = -1;
	private long timeStamp = -1L; // System.currentTimeMillis()
	private long timeSinceLastAction = -1L; // measured from end of last action to begin of current action
	private long timeElapsedStep = -1L; // measured from begin of current command to end of current command
	private Type typeOfLog;
	private Cmd cmd;
	private String param1;
	private String param2;
	private String returnValue;
	@JsonIgnore
	private Object returnObject;
	private Throwable issue;

	/**
	 * Empty Default constructor to be used by de-serialization.
	 */
	public Step( ) {
		// no-op
	}

	public Step(Type typeOfLog, int stepNumber, Cmd cmd) {
		this.recordNumber = Step.lastRecordNumber++;
		this.typeOfLog = typeOfLog;
		this.stepNumber = stepNumber;
		this.cmd = cmd;
		this.timeStamp = System.currentTimeMillis();
		
		switch(typeOfLog) {
		case BeforeAction:
			timeStampsForBeginAction();
			timeStampsForBeginStep();
			break;
		case AfterAction:
			timeStampsForAfterAction();
			timeStampsForAfterStep();
			break;
		case BeforeGather:
			timeStampsForBeginStep();
			break;
		case AfterGather:
			timeStampsForAfterStep();
			break;
		default:
		}
	}

	public int getRecordNumber() {
		return recordNumber;
	}

	public void setRecordNumber(int recordNumber) {
		this.recordNumber = recordNumber;
	}

	public int getStepNumber() {
		return stepNumber;
	}
	
	public void setStepNumber(int stepNumber) {
		this.stepNumber = stepNumber;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public long getTimeSinceLastAction() {
		return timeSinceLastAction;
	}

	public void setTimeSinceLastAction(long timeSinceLastStep) {
		this.timeSinceLastAction = timeSinceLastStep;
	}

	public long getTimeElapsedStep() {
		return timeElapsedStep;
	}

	public void setTimeElapsedStep(long timeElapsedStep) {
		this.timeElapsedStep = timeElapsedStep;
	}

	public Type getTypeOfLog() {
		return typeOfLog;
	}

	public void setTypeOfLog(Type typeOfLog) {
		this.typeOfLog = typeOfLog;
	}

	public Cmd getCmd() {
		return cmd;
	}

	public void setCmd(Cmd cmd) {
		this.cmd = cmd;
	}

	public String getParam1() {
		return param1;
	}

	public void setParam1(String param1) {
		this.param1 = param1;
	}

	public String getParam2() {
		return param2;
	}

	public void setParam2(String param2) {
		this.param2 = param2;
	}

	public String getReturnValue() {
		return returnValue;
	}

	public void setReturnValue(String returnValue) {
		this.returnValue = returnValue;
	}

	public Object getReturnObject() {
		return returnObject;
	}

	public void setReturnObject(Object returnObject) {
		this.returnObject = returnObject;
	}

	public Throwable getIssue() {
		return issue;
	}

	public void setIssue(Throwable issue) {
		this.issue = issue;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("stepno:").append(stepNumber).append(",");
		buffer.append("type:").append(typeOfLog).append(",");
		buffer.append("timestamp:").append(timeStamp).append(" ms,");
		buffer.append("cmd:").append(cmd);
		if (param1 != null) {
			buffer.append(",").append("param1:").append(param1);
		}
		if (param2 != null) {
			buffer.append(",").append("param2:").append(param2);
		}
		if (returnValue != null) {
			buffer.append(",").append("returned:").append(returnValue).append(",");
		}
		if (returnObject != null) {
			buffer.append(",").append("returned:").append(returnObject.toString()).append(",");
		}
		if (timeSinceLastAction != -1L) {
			buffer.append(",").append("since last step:").append(formattedNanoTime(timeSinceLastAction));
		}
		if (timeElapsedStep != -1L) {
			buffer.append(",").append("executed in:").append(formattedNanoTime(timeElapsedStep));
		}
		if (issue != null) {
			buffer.append(",").append("issue:").append(issue.getMessage());
		}
		
		return buffer.toString();
	}

	public static String formattedNanoTime(long duration) {
		String timeString = String.format("%d sec %d ms", TimeUnit.NANOSECONDS.toSeconds(duration),
				TimeUnit.NANOSECONDS.toMillis(duration)
						- TimeUnit.SECONDS.toMillis(TimeUnit.NANOSECONDS.toSeconds(duration)));
		return timeString;
	}

	private void timeStampsForBeginAction() {
		if (stepNumber > 1) {
			timeSinceLastAction = System.nanoTime() - timeMarkerSinceLastStep;
		}
	}

	private void timeStampsForAfterAction() {
		timeMarkerSinceLastStep = System.nanoTime();
	}

	private void timeStampsForBeginStep() {
		timeMarkerElapsedStep = System.nanoTime();
	}

	private void timeStampsForAfterStep() {
		timeElapsedStep = System.nanoTime() - timeMarkerElapsedStep;
	}

	public static String getLocatorFromWebElement(WebElement elem) {
		return (elem != null) ? getLocatorFromWebElement(elem.toString()) : null;
	}

	public static String getLocatorFromWebElement(String locator) {
		if (locator == null)
			return null;

		// sample string:
		// "[[RemoteWebDriver: firefox on WINDOWS (a66f78e9668e4aa3b066239459f969fe)] -> xpath: .//*[@id='Country__c_body']/table/tbody/tr[2]/th/a]"
		Pattern outerPattern = Pattern.compile("(\\[\\[.+\\] -> )(.+)\\]");
		Matcher outerMatcher = outerPattern.matcher(locator);
		if (!outerMatcher.matches()) {
			// return toString() as-is
			return locator;
		}

		// try to get the locator
		locator = locator.substring(outerMatcher.start(2), outerMatcher.end(2));
		// sample string:
		// "xpath: .//*[@id='Country__c_body']/table/tbody/tr[2]/th/a]"
		Pattern innerPattern = Pattern.compile("(\\S+): (.+)");
		Matcher innerMatcher = innerPattern.matcher(locator);
		boolean isLinkText = false;
		if (!innerMatcher.matches()) {
			innerPattern = Pattern.compile("(link text): (.+)");
			innerMatcher = innerPattern.matcher(locator);
			if (innerMatcher.matches()) {
				isLinkText = true;
			} else {
				// return what we got with the outer matcher
				return locator;
			}
		}

		// build the @FindBy string
		StringBuilder sb = new StringBuilder();
		sb.append("By.");
		// append locator type: "xpath"
		String locatorType = (isLinkText) ? "linkText" : locator.substring(innerMatcher.start(1), innerMatcher.end(1));
		sb.append(locatorType).append("(\"");
		// append locator itself: ".//*[@id='Country__c_body']/table/tbody/tr[2]/th/a]"
		sb.append(locator.substring(innerMatcher.start(2), innerMatcher.end(2))).append("\")");
		return sb.toString();
	}

	public static String getLocatorFromBy(By by) {
		return (by != null) ? getLocatorFromBy(by.toString()) : null;
	}

	public static String getLocatorFromBy(String locator) {
		if (locator == null)
			return null;
		// sample string:
		// "By.xpath: .//*[@id='thePage:j_id39:searchblock:test:j_id45_lkwgt']/img"
		Pattern pattern = Pattern.compile("By.(\\S+): (.+)");
		Matcher matcher = pattern.matcher(locator);
		if (!matcher.matches()) {
			// return what we got as-is
			return locator;
		}

		// build the @FindBy string
		StringBuilder sb = new StringBuilder();
		sb.append("By.");
		// append locator type: "xpath"
		sb.append(locator.substring(matcher.start(1), matcher.end(1))).append("(\"");
		// append locator itself:
		// ".//*[@id='thePage:j_id39:searchblock:test:j_id45_lkwgt']/img"
		sb.append(locator.substring(matcher.start(2), matcher.end(2))).append("\")");
		return sb.toString();
	}
}
