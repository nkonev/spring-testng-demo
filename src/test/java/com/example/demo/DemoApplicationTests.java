package com.example.demo;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = DemoApplication.class)
public class DemoApplicationTests extends AbstractTestNGSpringContextTests {

	private static final Logger LOGGER = LoggerFactory.getLogger(DemoApplicationTests.class);

	private final static OkHttpClient client = new OkHttpClient.Builder()
			.readTimeout(60, TimeUnit.SECONDS)
			.connectTimeout(60 / 2, TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS)
			.cache(null)
			.build();

	private String baseUrl;

	@Autowired
	private MyBean myBean;

	@BeforeSuite(alwaysRun = true)
	@Parameters({"baseUrl", "timeoutToStartSec", "kubernetesCheckTimes", "sleepBetweenRetry"})
	public void awaitServerReady(String baseUrl, int timeoutToStartSec, int kubernetesCheckTimes, int sleepBetweenRetry) {
		LOGGER.info("Start waiting for start");
		FailoverUtils.retry(timeoutToStartSec, () -> {
			try {
				for (int i = 0; i < kubernetesCheckTimes; ++i) {
					final Request request = new Request.Builder()
							.url(baseUrl + "/api/post?size=1")
							.build();
					LOGGER.info("Requesting " + (i + 1) + "/" + kubernetesCheckTimes + " " + request.toString());
					final Response response = client.newCall(request).execute();
					final String version = response.body().string();
					Assert.assertEquals(200, response.code());
					LOGGER.info("Successful get posts: \n" + version);
				}
				return null;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}, sleepBetweenRetry);
		this.baseUrl = baseUrl;
	}

	@Test
	public void testVersionGit() throws IOException {
		final Request request = new Request.Builder()
				.url(baseUrl+"/git.json")
				.build();

		final Response response = client.newCall(request).execute();
		final String json = response.body().string();
		LOGGER.info("Git version response: {}", json);

		ReadContext ctx = JsonPath.parse(json);
		String version = ctx.read("$.['build.version']", String.class);

		Assert.assertNotNull(version);
		Assert.assertFalse(version.isEmpty());
	}

	@Test
	public void testPost() throws IOException {
		final Request request = new Request.Builder()
				.url(baseUrl+"/api/post?size=2")
				.build();

		final Response response = client.newCall(request).execute();
		final String json = response.body().string();
		LOGGER.info("Git post response: {}", json);

		ReadContext ctx = JsonPath.parse(json);
		int numberOfPosts = ctx.read("$.data.length()", int.class);

		Assert.assertEquals(myBean.getPostNumber(), numberOfPosts);
	}


}
