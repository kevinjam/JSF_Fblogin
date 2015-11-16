/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 *
 * @author kevinjanvier
 */
@WebServlet("*.sec")
public class SecurityServlet extends HttpServlet{
    
    private static final long serialVersionUID = 8071426090770097330L;

	public SecurityServlet() {
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		System.out.println("*** Called SecurityServlet");
		HttpSession httpSession = request.getSession();
		String faceCode = request.getParameter("code");
		String state = request.getParameter("state");
		String accessToken = getFacebookAccessToken(faceCode);
		String email = getUserMailAddressFromJsonResponse(accessToken, httpSession);
		String sessionID = httpSession.getId();
		if (state.equals(sessionID)){
			try {
				//do some specific user data operation like saving to DB or login user
				//request.login(email, "somedefaultpassword");
			} catch (Exception e) {
				e.printStackTrace();
				response.sendRedirect(request.getContextPath() +"/facebookError.html");
				return;
			}
			response.sendRedirect(request.getContextPath() +"/welcomePrimefaces.xhtml");
		} else {
			System.err.println("CSRF protection validation");
		}
	}

	private String getFacebookAccessToken(String faceCode){
		String token = null;
		if (faceCode != null && ! "".equals(faceCode)) {
			String appId = "303525776508598";
			String redirectUrl = "http://localhost:8080/Facebook_Login/index.sec";
			String faceAppSecret = "9a11d42ee3086b9e62affd4005e9e85d";
			String newUrl = "https://graph.facebook.com/oauth/access_token?client_id="
					+ appId + "&redirect_uri=" + redirectUrl + "&client_secret=" 
					+ faceAppSecret + "&code=" + faceCode;
			HttpClient httpclient = new DefaultHttpClient();
			try {
				HttpGet httpget = new HttpGet(newUrl);
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				String responseBody = httpclient.execute(httpget, responseHandler);
				token = StringUtils.removeEnd
						(StringUtils.removeStart(responseBody, "access_token="), "&expires=5180795");
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				httpclient.getConnectionManager().shutdown();
			}
		}
		return token;
	}

	private String getUserMailAddressFromJsonResponse(String accessToken, HttpSession httpSession) {
		String email = null;
		HttpClient httpclient = new DefaultHttpClient();
		try {
//			if (accessToken != null && ! "".equals(accessToken)) {
                        if (accessToken != null ) {
				String newUrl = "https://graph.facebook.com/me/friends?access_token=" + accessToken;
				httpclient = new DefaultHttpClient();
				HttpGet httpget = new HttpGet(newUrl);
				System.out.println("Get info from face --> executing request: " + httpget.getURI());
				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				
                                String responseBody = httpclient.execute(httpget, responseHandler);
				JSONObject json = (JSONObject)JSONSerializer.toJSON(responseBody);
                                
				System.out.println("from json " +json.toString());
                                
                                String facebookId = json.getString("id");
				String firstName = json.getString("first_name");
				String lastName = json.getString("last_name");
				email= json.getString("email");
                                
				//put user data in session
				httpSession.setAttribute("FACEBOOK_USER", firstName+" "
						+lastName+", facebookId:" + facebookId);
				
			} else {
				System.err.println("Token za facebook je null");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
		return email;
	}
    
}
