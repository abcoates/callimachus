package org.callimachusproject.webdriver.pages;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import org.callimachusproject.webdriver.helpers.WebBrowserDriver;
import org.openqa.selenium.By;

public class CalliPage {
	protected WebBrowserDriver driver;

	public CalliPage(WebBrowserDriver driver) {
		this.driver = driver;
	}

	public CalliPage waitUntilTitle(String title) {
		driver.waitUntilTextPresent(By.tagName("h1"), title);
		return this;
	}

	public Login openLogin() {
		driver.focusInTopWindow();
		driver.click(By.id("login-link"));
		return page(Login.class);
	}

	public CalliPage logout() {
		driver.focusInTopWindow();
		driver.click(By.cssSelector("i.icon-cog"));
		driver.click(By.id("logout-link"));
		return page();
	}

	public FolderView openHomeFolder() {
		driver.focusInTopWindow();
		driver.click(By.cssSelector("i.icon-cog"));
		driver.click(By.linkText("Home folder"));
		return page(FolderView.class);
	}

	public FolderView openCurrentFolder() {
		driver.focusInTopWindow();
		driver.navigateTo("./?view");
		return page(FolderView.class);
	}

	public CalliPage open(String ref) {
		driver.focusInTopWindow();
		driver.navigateTo(ref);
		return page();
	}

	public <P> P openEdit(Class<P> pageClass) {
		driver.focusInTopWindow();
		driver.click(By.linkText("Edit"));
		return page(pageClass);
	}

	public HistoryPage openHistory() {
		driver.focusInTopWindow();
		driver.click(By.linkText("History"));
		return page(HistoryPage.class);
	}

	public DiscussionPage openDiscussion() {
		driver.focusInTopWindow();
		driver.click(By.linkText("Discussion"));
		return page(DiscussionPage.class);
	}

	public CalliPage back() {
		driver.navigateBack();
		return page();
	}

	public CalliPage page() {
		driver.waitForScript();
		return new CalliPage(driver);
	}

	public <P> P page(Class<P> pageClass) {
		driver.waitForScript();
		try {
			try {
				return pageClass.getConstructor(WebBrowserDriver.class).newInstance(driver);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		} catch (Error e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new UndeclaredThrowableException(e);
		}
	}

}
