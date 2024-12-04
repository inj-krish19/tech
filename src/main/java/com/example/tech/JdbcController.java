package com.example.tech;

import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@Controller
public class JdbcController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static Integer start = 1;

    @Autowired
    private HttpSession session;
    
    Dotenv dotenv = Dotenv.load();
    
    String userExist = "";

    @GetMapping("/secret")
    public String secret( HttpServletRequest request) {
    	
    	List<String> admin_username = new ArrayList<>( List.of(
        		dotenv.get("ADMIN_USERNAME")
        	) );
        
    	
		Enumeration<String> attributeNames = request.getSession().getAttributeNames();
		System.out.println("Printing all session variables:");
	
		while (attributeNames.hasMoreElements()) {
		    String attributeName = attributeNames.nextElement();
		    Object attributeValue = request.getSession().getAttribute(attributeName);
		    System.out.println(attributeName + " = " + attributeValue);
	
		    // Check if the attribute is the SPRING_SECURITY_CONTEXT
		    if ("SPRING_SECURITY_CONTEXT".equals(attributeName)) {
		        org.springframework.security.core.context.SecurityContext securityContext =
		            (org.springframework.security.core.context.SecurityContext) attributeValue;
		        
		        // Extract Authentication object
		        org.springframework.security.core.Authentication authentication = securityContext.getAuthentication();
		        
		        // Check if the user is authenticated and extract the principal (username)
		        if (authentication != null && authentication.isAuthenticated()) {
		            Object principal = authentication.getPrincipal();
		            
		            String username = null;
		            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
		                username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
		            } else {
		                username = principal.toString();  // Fallback if principal is a string
		            }
		            
		            // Print and store the username
		            System.out.println("Username from SPRING_SECURITY_CONTEXT: " + username);
		            
		            if( admin_username.contains(username) ) {
		            	request.getSession().setAttribute("authorId", -1);
			            return "redirect:/admin";  
		            }
		            
		            String sql = "SELECT authorId FROM Blogger WHERE username = ?";
		            Integer authorId = jdbcTemplate.queryForObject(sql, Integer.class, username );

		            request.getSession().setAttribute("authorId", authorId);
				   		            
		            return "redirect:/login?success=true";  
		        }		    
		    }
		}

		return "redirect:/login?error=false";
    }
    
    @GetMapping("/") // Maps to the root URL (http://localhost:8080/)
    public String home(Model model, Principal principal) {
        
    	if (principal != null && userExist != null) {
            model.addAttribute("loggedInUser", userExist = principal.getName()); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
    	String categorySql = "SELECT name FROM Category";
        List<String> categories = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("topics", categories);

        String postSql = """
                SELECT p.articleid, 
                       p.title, 
                       p.description, 
                       p.likes, 
                       p.dislikes, 
                       p.viewscount, 
                       p.commentscount AS comments, 
                       p.updatedat, 
                       p.postmedia AS image,
                       u.name AS name, 
                       u.username AS username, 
                       u.bio AS bio, 
                       c.name AS category
                FROM Post p 
                JOIN Blogger u ON p.primaryAuthor = u.authorid 
                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
                JOIN Category c ON pca.categoryid = c.categoryid
                LIMIT 5
            """;

            List<Map<String, Object>> posts = jdbcTemplate.query(postSql, (rs, rowNum) -> {
                Map<String, Object> post = new HashMap<>();
                int articleId = rs.getInt("articleid");
                
                post.put("articleid", articleId);
                post.put("title", rs.getString("title"));
                post.put("description", rs.getString("description"));
                post.put("likes", rs.getInt("likes"));
                post.put("dislikes", rs.getInt("dislikes"));
                post.put("viewscount", rs.getInt("viewscount"));
                post.put("comments", rs.getInt("comments"));
                post.put("updatedat", rs.getTimestamp("updatedat"));
                post.put("name", rs.getString("name"));
                post.put("username", rs.getString("username"));
                post.put("bio", rs.getString("bio"));
                post.put("category", rs.getString("category"));
                post.put("image", rs.getString("image"));

                // Separate query to get keywords for the current article
                String keywordQuery = """
                		SELECT name FROM Keyword k 
                		JOIN KeywordAssignment ka 
                		ON k.keywordid = ka.keywordid 
                		WHERE ka.articleid = ?
                	""";
            	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Integer) articleId);
            	post.put("keywords", keywords);

                return post;
            });

        model.addAttribute("posts", posts);

        System.out.print(posts);

    	List<String> colors = new ArrayList<>(
    			List.of(
    				"green", "blue","red", "purple", "lightgreen", "lightblue", "pink", "aliceblue", "black", "cyan",  "yellow", "brown"
    				)
    		);
    	
    	model.addAttribute("colors", colors);
        
        if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
        return "home"; // Home page
    }
    
    @GetMapping("/myposts")
    public String myposts(Model model, HttpServletRequest request) {
        // Check for logged-in user and set attribute
        if (this.userExist != null && !this.userExist.isEmpty()) {
            model.addAttribute("loggedInUser", userExist);
        } else {
            model.addAttribute("loggedInUser", null);
        }

        String categorySql = "SELECT name FROM Category";
        List<String> categories = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("topics", categories);

        String postSql = """
                SELECT p.articleid, 
                       p.title, 
                       p.description, 
                       p.likes, 
                       p.dislikes, 
                       p.viewscount, 
                       p.commentscount AS comments, 
                       p.updatedat, 
                       p.postmedia AS image,
                       u.name AS name, 
                       u.username AS username, 
                       u.bio AS bio, 
                       c.name AS category
                FROM Post p 
                JOIN Blogger u ON p.primaryAuthor = u.authorid 
                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
                JOIN Category c ON pca.categoryid = c.categoryid
                WHERE u.authorid = ? LIMIT 5
            """;

            List<Map<String, Object>> posts = jdbcTemplate.query(postSql, (rs, rowNum) -> {
                Map<String, Object> post = new HashMap<>();
                int articleId = rs.getInt("articleid");
                
                post.put("articleid", articleId);
                post.put("title", rs.getString("title"));
                post.put("description", rs.getString("description"));
                post.put("likes", rs.getInt("likes"));
                post.put("dislikes", rs.getInt("dislikes"));
                post.put("viewscount", rs.getInt("viewscount"));
                post.put("comments", rs.getInt("comments"));
                post.put("updatedat", rs.getTimestamp("updatedat"));
                post.put("name", rs.getString("name"));
                post.put("username", rs.getString("username"));
                post.put("bio", rs.getString("bio"));
                post.put("category", rs.getString("category"));
                post.put("image", rs.getString("image"));

                // Separate query to get keywords for the current article
                String keywordQuery = """
                		SELECT name FROM Keyword k 
                		JOIN KeywordAssignment ka 
                		ON k.keywordid = ka.keywordid 
                		WHERE ka.articleid = ?
                	""";
            	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Integer) articleId);
            	post.put("keywords", keywords);

                return post;
            }, (Integer) request.getSession().getAttribute("authorId"));

        model.addAttribute("posts", posts);

        System.out.print(posts);

    	List<String> colors = new ArrayList<>(
    			List.of(
    				"green", "blue","red", "purple", "lightgreen", "lightblue", "pink", "aliceblue", "black", "cyan",  "yellow", "brown"
    				)
    		);
    	
    	model.addAttribute("colors", colors);
        
        if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
        return "show"; 
    }

    
    // Get all bloggers
    @GetMapping("/bloggers")
    public String getAllBloggers(Model model) {
        String sql = "SELECT * FROM Blogger";
        List<Map<String, Object>> bloggers = jdbcTemplate.queryForList(sql);
        model.addAttribute("bloggers", bloggers);
        
        if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
        return "bloggers";
    }
    
    // Show all post
    @GetMapping("/posts")
    public String showpost(Model model) {
    	
    	String categorySql = "SELECT name FROM Category";
        List<String> categories = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("topics", categories);

        String postSql = """
                SELECT p.articleid, 
                       p.title, 
                       p.description, 
                       p.likes, 
                       p.dislikes, 
                       p.viewscount, 
                       p.commentscount AS comments, 
                       p.updatedat, 
                       p.postmedia AS image,
                       u.name AS name, 
                       u.username AS username, 
                       u.bio AS bio, 
                       c.name AS category
                FROM Post p 
                JOIN Blogger u ON p.primaryAuthor = u.authorid 
                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
                JOIN Category c ON pca.categoryid = c.categoryid
            """;

            List<Map<String, Object>> posts = jdbcTemplate.query(postSql, (rs, rowNum) -> {
                Map<String, Object> post = new HashMap<>();
                int articleId = rs.getInt("articleid");
                
                post.put("articleid", articleId);
                post.put("title", rs.getString("title"));
                post.put("description", rs.getString("description"));
                post.put("likes", rs.getInt("likes"));
                post.put("dislikes", rs.getInt("dislikes"));
                post.put("viewscount", rs.getInt("viewscount"));
                post.put("comments", rs.getInt("comments"));
                post.put("updatedat", rs.getTimestamp("updatedat"));
                post.put("name", rs.getString("name"));
                post.put("username", rs.getString("username"));
                post.put("bio", rs.getString("bio"));
                post.put("category", rs.getString("category"));
                post.put("image", rs.getString("image"));

                // Separate query to get keywords for the current article
                String keywordQuery = """
                		SELECT name FROM Keyword k 
                		JOIN KeywordAssignment ka 
                		ON k.keywordid = ka.keywordid 
                		WHERE ka.articleid = ?
                	""";
            	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Integer) articleId);
            	post.put("keywords", keywords);

                return post;
            });

        model.addAttribute("posts", posts);

        System.out.print(posts);

    	List<String> colors = new ArrayList<>(
    			List.of(
    				"green", "blue","red", "purple", "lightgreen", "lightblue", "pink", "aliceblue", "black", "cyan",  "yellow", "brown"
    				)
    		);
    	
    	model.addAttribute("colors", colors);
        
        if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
        return "show"; // Assuming there's a Thymeleaf template named "show"
    }
    
    @PostMapping("/review")
    public String reviewPost(Model model, String title, String category, String description, String selectedKeywords, HttpServletRequest request ) {
        
    	String sql = "SELECT username,name,bio FROM Blogger where authorId=?";
    	
    	Map<String,Object> user = jdbcTemplate.queryForMap(sql, (Integer) request.getSession().getAttribute("authorId") );
    	
    	Map<String, Object> postData = new HashMap<>();
        postData.put("title", title);
        postData.put("category", category);
        description = description.replaceAll("<[^>]*>", "").trim();
        postData.put("description", description);
        
        postData.put("username", user.get("username") );
        postData.put("name", user.get("name") );
        postData.put("bio", user.get("bio") );
        
        String keyword = selectedKeywords;
        System.out.print("Keyword " + keyword);
        List<String> keywords = new ArrayList<>();
        List<Integer> buttonIndex = new ArrayList<>();
        StringTokenizer tokens = new StringTokenizer(keyword, ",");

        keyword = "";
        
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            String[] splitToken = token.split("-");
            if (splitToken.length > 1) {
            	keywords.add(splitToken[0]);
            	keyword += splitToken[0] +  ",";// Add the token (keyword) to the keywords list
                buttonIndex.add(Integer.valueOf(splitToken[1]));
            }
        }

        
        /* while( tokens.hasMoreTokens() ) {
        	keywords.add( tokens.nextToken() );
        	buttonIndex.add( Integer.valueOf( tokens.nextToken().split("-")[1] ) );      	
        }	*/
        
        System.out.print(buttonIndex);
        
        // Add the list to the post map
        postData.put("keyword", keyword);
        postData.put("keywords", keywords);
        postData.put("buttonIndex", buttonIndex);
        postData.put("image", null);

        List<String> colors = new ArrayList<>(
			List.of(
				"purple", "cyan",  "green", "red", "blue", "black", "aliceblue", "yellow", "brown", "lightgreen", "lightblue", "pink"
				)
		);
    	
    	System.out.println("Keywords : " + keywords);
    	System.out.println("Indices : " + buttonIndex);
    	
    	model.addAttribute("colors", colors);
        // Add the Map to the model
        model.addAttribute("post", postData);
        
        if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
        return "reviewPost"; // Assuming there's a Thymeleaf template named "show"
    }
    
    // @GetMapping("/review")
    // public String review(Model model, String title, String category, String description) {
    //     Map<String, Object> postData = new HashMap<>();
    //     postData.put("title", "title");
    //     postData.put("category", "category");
    //     postData.put("description", "description");
        
    //     // Add the Map to the model
    //     model.addAttribute("post", postData);
        
    //     return "reviewPost"; // Assuming there's a Thymeleaf template named "show"
    // }

    @GetMapping("/post/{id}")
    public String viewPost(Model model, @PathVariable String id) {
        String sql = """
                SELECT p.articleid, 
                p.title, 
                p.description, 
                p.likes, 
                p.dislikes, 
                p.viewscount, 
                p.commentscount AS comments, 
                p.updatedat, 
                p.postmedia AS image,
                u.name AS name, 
                u.username AS username, 
                u.bio AS bio, 
                c.name AS category
         FROM Post p 
         JOIN Blogger u ON p.primaryAuthor = u.authorid 
         JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
         JOIN Category c ON pca.categoryid = c.categoryid
         WHERE articleid = ?
        """;
        
        List<Map<String, Object>> post = jdbcTemplate.queryForList(sql, id);
        model.addAttribute("post", post);
        
        if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
        return "viewPost"; // Assuming there's a Thymeleaf template named "viewPost"
    }

    
    
    @GetMapping("/filter/category/{category}")
    public String filterCategoryPost(Model model, @PathVariable String category) {
        // Fetch category ID based on name (case-insensitive)
        String sql = "SELECT categoryId FROM Category WHERE LOWER(name) LIKE LOWER(?)";
        List<Integer> categories = jdbcTemplate.queryForList(sql, Integer.class, "%" + category + "%");

        if (categories.isEmpty()) {
            model.addAttribute("error", "No posts found in this category.");
            model.addAttribute("posts", null);
            return "filter"; 
        }

        int categoryId = categories.get(0);

        // Query to retrieve posts along with keywords
        sql = """
                SELECT p.articleid, 
                p.title, 
                p.description, 
                p.likes, 
                p.dislikes, 
                p.viewscount, 
                p.commentscount AS comments, 
                p.updatedat, 
                p.postmedia AS image,
                u.name AS name, 
                u.username AS username, 
                u.bio AS bio, 
                c.name AS category
         FROM Post p 
         JOIN Blogger u ON p.primaryAuthor = u.authorid 
         JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
         JOIN Category c ON pca.categoryid = c.categoryid
         WHERE c.categoryid = ? LIMIT 5
     """;

        List<String> colors = new ArrayList<>(
    			List.of(
    				"purple", "cyan",  "green", "red", "blue", "black", "aliceblue", "yellow", "brown", "lightgreen", "lightblue", "pink"
    				)
    		);
    	model.addAttribute("colors", colors);
        
     List<Map<String, Object>> posts = jdbcTemplate.query(sql, (rs, rowNum) -> {
         Map<String, Object> post = new HashMap<>();
         int articleId = rs.getInt("articleid");
         
         post.put("articleid", articleId);
         post.put("title", rs.getString("title"));
         post.put("description", rs.getString("description"));
         post.put("likes", rs.getInt("likes"));
         post.put("dislikes", rs.getInt("dislikes"));
         post.put("viewscount", rs.getInt("viewscount"));
         post.put("comments", rs.getInt("comments"));
         post.put("updatedat", rs.getTimestamp("updatedat"));
         post.put("name", rs.getString("name"));
         post.put("username", rs.getString("username"));
         post.put("bio", rs.getString("bio"));
         post.put("category", rs.getString("category"));
         post.put("image", rs.getString("image"));

         // Separate query to get keywords for the current article
         String keywordQuery = """
         		SELECT name FROM Keyword k 
         		JOIN KeywordAssignment ka 
         		ON k.keywordid = ka.keywordid 
         		WHERE ka.articleid = ?
         	""";
     	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Integer) articleId);
     	post.put("keywords", keywords);

         return post;
     }, categoryId);

     	model.addAttribute("posts", posts);


        model.addAttribute("topic", category);
        model.addAttribute("loggedInUser", (userExist != null && !userExist.isEmpty()) ? userExist : null);

        return "filter"; 
    }
    
    
    @GetMapping("/filter/keyword/{keyword}")
    public String filterKeywordpost(Model model, @PathVariable String keyword) {
    	
        String sql = "SELECT keywordId FROM Keyword WHERE LOWER(name) LIKE LOWER(?)";
        List<Integer> keywords = jdbcTemplate.queryForList(sql, Integer.class, "%" + keyword + "%");

        if (keywords.isEmpty()) {
            model.addAttribute("error", "No posts found in this category.");
            return "filter"; // Return with error message if no category found
        }

        int keywordId = keywords.get(0);

        // Fetch posts based on keyword ID
        sql = """
            SELECT 
                p.articleid, 
                p.title, 
                p.description, 
                p.likes, 
                p.dislikes, 
                p.viewscount, 
                p.commentscount AS comments, 
                p.updatedat, 
                p.postmedia AS image,
                u.name AS name, 
                u.username AS username, 
                u.bio AS bio, 
                c.name AS category,
                STRING_AGG(k.name, ',') AS keywords
            FROM Post p 
            JOIN Blogger u ON p.primaryAuthor = u.authorid 
            JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
            JOIN Category c ON pca.categoryid = c.categoryid 
            LEFT JOIN keywordAssignment pka ON p.articleid = pka.articleid 
            LEFT JOIN Keyword k ON pka.keywordid = k.keywordid
            WHERE k.keywordid = ?
            GROUP BY p.articleid, p.title, p.description, p.likes, p.dislikes, 
                     p.viewscount, p.commentscount, p.updatedat, p.postmedia,
                     u.name, u.username, u.bio, c.name
            """;
        
        List<String> colors = new ArrayList<>(
    			List.of(
    				"purple", "cyan",  "green", "red", "blue", "black", "aliceblue", "yellow", "brown", "lightgreen", "lightblue", "pink"
    				)
    		);
    	model.addAttribute("colors", colors);

        List<Map<String, Object>> posts = jdbcTemplate.query(sql, (rs, rowNum) -> {
        	Map<String, Object> post = new HashMap<>();
            int articleId = rs.getInt("articleid");
            
            post.put("articleid", articleId);
            post.put("title", rs.getString("title"));
            post.put("description", rs.getString("description"));
            post.put("likes", rs.getInt("likes"));
            post.put("dislikes", rs.getInt("dislikes"));
            post.put("viewscount", rs.getInt("viewscount"));
            post.put("comments", rs.getInt("comments"));
            post.put("updatedat", rs.getTimestamp("updatedat"));
            post.put("name", rs.getString("name"));
            post.put("username", rs.getString("username"));
            post.put("bio", rs.getString("bio"));
            post.put("category", rs.getString("category"));
            post.put("image", rs.getString("image"));

            // Separate query to get keywords for the current article
            String keywordQuery = """
            		SELECT name FROM Keyword k 
            		JOIN KeywordAssignment ka 
            		ON k.keywordid = ka.keywordid 
            		WHERE ka.articleid = ?
            	""";
        	List<String> keywordsOfPost = jdbcTemplate.queryForList(keywordQuery, String.class, (Integer) articleId);
        	post.put("keywords", keywordsOfPost);
        	
        	return post;
        } ,keywordId);

        // Debugging (optional)
        System.out.println("Executed Query: \n" + sql);
        System.out.println("Keyword: " + keyword + " | Posts: " + posts);

        // Add posts to the model
        if (posts.isEmpty()) {
            model.addAttribute("error", "No posts found for this category.");
            model.addAttribute("posts", null);
        } else {
            model.addAttribute("posts", posts);
        }

        // Add logged-in user info
        if (userExist != null && !userExist.isEmpty()) {
            model.addAttribute("loggedInUser", userExist);
        } else {
            model.addAttribute("loggedInUser", null);
        }

        model.addAttribute("topic", keyword);
        return "filter";// Assuming there's a Thymeleaf template named "show"
    }

    
    @GetMapping("/create_post") // Maps to /create (http://localhost:8080/create)
    public String createPost(Model model) {
    	
    	String sql = "SELECT name FROM Category";
    	List<String> categories = jdbcTemplate.queryForList(sql, String.class);
    	model.addAttribute("categories", categories);
    	
    	sql = "SELECT name FROM Keyword";
    	categories = jdbcTemplate.queryForList(sql, String.class);
    	model.addAttribute("keywords", categories);
    	
    	List<String> colors = new ArrayList<>(
    			List.of(
    				"purple", "cyan",  "green", "red", "blue", "black", "aliceblue", "yellow", "brown", "lightgreen", "lightblue", "pink"
    				)
    		);
    	
    	model.addAttribute("colors", colors);
    	
    	if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	
        return "createPost"; // Create post page
    }
    

    @GetMapping("/profile")
    public String getProfile(HttpServletRequest request, Model model) {
        // Retrieve session and enumerate attributes
        Enumeration<String> attributeNames = request.getSession().getAttributeNames();
        System.out.println("Printing all session variables:");
        
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = request.getSession().getAttribute(attributeName);
            System.out.println(attributeName + " = " + attributeValue);
        }

        // Fetch userId from session
        Integer userId = (Integer) request.getSession().getAttribute("authorId");
        if (userId == null) {
            System.out.println("User not logged in. Redirecting to login.");
            model.addAttribute("error", "You need to log in to view your profile.");
            return "redirect:/login";
        }
        
        // Query user data from the database
        String sql = "SELECT * FROM Blogger WHERE authorId = ?";
        List<Map<String, Object>> user = jdbcTemplate.queryForList(sql, userId);

        if (user.isEmpty()) {
            model.addAttribute("error", "User not found.");
            return "error"; // Redirect to an error page or show a meaningful message
        }

        // Add user data to the model
        model.addAttribute("user", user.get(0));

        // Add logged-in username if available
        String loggedInUser = (String) request.getSession().getAttribute("loggedInUser");
        model.addAttribute("loggedInUser", loggedInUser != null ? loggedInUser : "Guest");

        return "profilemanagement"; // Return the profile management view
    }
    
    @PostMapping("/search")
    public String searchKeyword(Model model, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            model.addAttribute("error", "Please enter a valid search term.");
            model.addAttribute("posts", null);
            return "filter";
        }

        String sql = """
            SELECT p.articleid, 
                   p.title, 
                   p.description, 
                   u.name AS name, 
                   u.username AS username, 
                   u.bio AS bio, 
                   c.name AS category
            FROM Post p
            JOIN Blogger u ON p.primaryauthor = u.authorid
            JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid
            JOIN Category c ON pca.categoryid = c.categoryid
            WHERE p.title ILIKE ? 
               OR p.description ILIKE ? 
               OR c.name ILIKE ?
        """;

        String likePattern = "%" + keyword + "%";

        // Execute query and retrieve posts
        List<Map<String, Object>> posts = jdbcTemplate.queryForList(sql, likePattern, likePattern, likePattern);

        if (posts.isEmpty()) {
            model.addAttribute("error", "No posts found for the search term.");
        } else {
            model.addAttribute("posts", posts);
        }

        // Pass keyword and logged-in user info to the view
        model.addAttribute("topic", keyword);
        model.addAttribute("loggedInUser", (userExist != null && !userExist.isEmpty()) ? userExist : null);

        return "filter";
    }

    
    @PostMapping("/search2")
    public String seacrhKeyword(Model model, String keyword ) {
    	
    	keyword = "'%" + keyword + "%'";
    	
    	String sql = """
    		    SELECT p.articleid, p.title, p.description, u.name AS name, u.username AS username, u.bio AS bio, c.name AS category
    		    FROM Post p
    		    JOIN Blogger u ON p.primaryauthor = u.authorid
    		    JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid
    		    JOIN Category c ON pca.categoryid = c.categoryid
    		    WHERE p.title ILIKE '%' || ? || '%' 
    		       OR p.description ILIKE '%' || ? || '%' 
    		       OR c.name ILIKE '%' || ? || '%';
    		""";

    		// Execute query with the keyword as a parameter
    		List<Map<String, Object>> post = jdbcTemplate.queryForList(sql, keyword, keyword, keyword);

    		// Add data to the model
    		model.addAttribute("post", post);
    		model.addAttribute("topic", keyword.substring(1, keyword.length() - 1));

    		if (this.userExist != "" && userExist != null ) {
                model.addAttribute("loggedInUser", userExist); // Add the logged-in username
            } else {
                model.addAttribute("loggedInUser", null); // No user logged in
            }
    		
    		// Return the "filter" view
    		return "filter";
    }
    
    @PostMapping("/doPost")
    public String makePost(Model model, String title, String category, String description, String keyword, HttpServletRequest request) {
        try {
        	
        	System.out.print( title + " " + category+ " " + description + " " + keyword);
            // Step 1: Clean the description
            description = description.replaceAll("<[^>]*>", "").trim();
            Integer primaryAuthorId = (Integer) request.getSession().getAttribute("authorId");;  // Assuming logged-in user ID

            // Step 2: Insert Post
            String insertPostSql = """
                INSERT INTO Post (articleid, title, description, likes, dislikes, commentscount, primaryauthor, viewscount, postmedia, publishedat, createdat, updatedat) 
                VALUES (?, ?, ?, 0, 0, 0, ?, 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;
            Integer newArticleId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(articleid), 0) + 1 FROM Post", Integer.class);
            int postRowsAffected = jdbcTemplate.update(insertPostSql, newArticleId, title, description, primaryAuthorId);
            System.out.println("Post Inserted. Rows affected: " + postRowsAffected);

            // Step 3: Insert into PostCategoryAssignment
            String getCategoryIdSql = "SELECT categoryid FROM Category WHERE LOWER(name) = LOWER(?)";
            Integer categoryId = null;
            try {
                categoryId = jdbcTemplate.queryForObject(getCategoryIdSql, Integer.class, category);
            } catch (EmptyResultDataAccessException e) {
                System.out.println("Category not found: " + category);
            }
            
            if (categoryId != null) {
            	
            	Integer postCategoryAssignmnetId = jdbcTemplate.queryForObject( """
            			SELECT COALESCE(MAX(postCategoryAssignmentId), 0) + 1 FROM PostCategoryAssignment
            			""", Integer.class);
            	
                String insertCategoryAssignmentSql = """
                    INSERT INTO PostCategoryAssignment ( articleid, categoryid, assignedby, createdat) 
                    VALUES ( ?, ?, ?, CURRENT_TIMESTAMP)
                """;
                
                jdbcTemplate.update(insertCategoryAssignmentSql, newArticleId, categoryId, primaryAuthorId);
                System.out.println("Category Assignment Inserted.");
            } else {
                throw new IllegalArgumentException("Category not found.");
            }

            // Step 4: Insert Keywords
            String[] keywordArray = keyword.split(",");
            
            if (keywordArray.length > 0) {
                for (String currentKeyword : keywordArray) {
                    // Get keywordId (we assume keywords are case-insensitive)
                    String getKeywordIdSql = "SELECT keywordId FROM Keyword WHERE LOWER(name) LIKE LOWER(?)";
                    Integer keywordId = null;
                    try {
                        keywordId = jdbcTemplate.queryForObject(getKeywordIdSql, Integer.class, "%" + currentKeyword.trim() + "%");
                    } catch (EmptyResultDataAccessException e) {
                        System.out.println("Keyword not found: " + currentKeyword);
                    }
                    
                    if (keywordId != null) {
                    	
                    	Integer keywordAssignmentId = jdbcTemplate.queryForObject( """
                    			SELECT COALESCE(MAX(keywordAssignmentId), 0) + 1 FROM KeywordAssignment
                    		""", Integer.class);
                    	
                        String insertKeywordAssignmentSql = """
                            INSERT INTO KeywordAssignment ( articleid, keywordid, assignedBy, createdat) 
                            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                        """;
                        jdbcTemplate.update(insertKeywordAssignmentSql, newArticleId, keywordId, primaryAuthorId);
                    }
                }
                System.out.println("Keyword Assignments Inserted.");
            }

            // Set logged-in user attribute
            if (this.userExist != null && !this.userExist.isEmpty()) {
                model.addAttribute("loggedInUser", userExist); // Add the logged-in username
            } else {
                model.addAttribute("loggedInUser", null); // No user logged in
            }

        } catch (Exception e) {
            model.addAttribute("error", "Error while creating post: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/create_post"; // Return to the post creation page in case of an error
        }

        return "redirect:/"; // Redirect to post listing page
    }

    
    /* Create a new post
    @PostMapping("/doPost")
    public String makePost(Model model, String title, String categoryName, String description, String keyword) {
        try {
            // Clean up the description by removing HTML tags and trimming
            description = description.replaceAll("<[^>]*>", "").trim();
            int primaryAuthorId = 1; // Change as per your logic

            // Generate a new article ID
            String getMaxIdSql = "SELECT COALESCE(MAX(articleid), 0) + 1 FROM Post";
            Integer newArticleId = jdbcTemplate.queryForObject(getMaxIdSql, Integer.class);

            // Insert the new post
            String insertPostSql = """
                INSERT INTO Post (articleid, title, description, likes, dislikes, commentscount, primaryauthor, viewscount, postmedia, publishedat, createdat, updatedat) 
                VALUES (?, ?, ?, 0, 0, 0, ?, 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;
            int postRowsAffected = jdbcTemplate.update(insertPostSql, newArticleId, title, description, primaryAuthorId);
            System.out.println("Post Inserted. Rows affected: " + postRowsAffected);

            // Insert into PostCategoryAssignment
            String getCategoryIdSql = "SELECT categoryid FROM Category WHERE  LOWER(name) LIKE LOWER(?)";
            Integer categoryId = jdbcTemplate.queryForObject(getCategoryIdSql, Integer.class, categoryName);

            String insertCategoryAssignmentSql = """
                INSERT INTO PostCategoryAssignment (postcategoryassignmentid, articleid, categoryid, assignedby, createdat) 
                VALUES ((SELECT COALESCE(MAX(postcategoryassignmentid), 0) + 1 FROM PostCategoryAssignment), ?, ?, ?, CURRENT_TIMESTAMP)
            """;
            jdbcTemplate.update(insertCategoryAssignmentSql, newArticleId, categoryId, primaryAuthorId);
            System.out.println("Category Assignment Inserted.");

            StringTokenizer tokens = new StringTokenizer(keyword, ",");
            List<String> keywords = new ArrayList<>();
            
            keyword = "";
            
            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();
                keywords.add(token);
            }
            
            // Insert into keywordAssignment
            if (keywords != null && keywords.size() > 0) {
                for (String currentKeyword : keywords) {
                    String getKeywordIdSql = "SELECT COALESCE(MAX(keywordid), 0) + 1 FROM Keyword WHERE LOWER(name) LIKE LOWER(?)";
                    Integer keywordId = jdbcTemplate.queryForObject(getKeywordIdSql, Integer.class, currentKeyword);

                    if( keywordId == null ) {
                    	continue;
                    }
                    
                    String insertKeywordAssignmentSql = """
                        INSERT INTO keywordAssignment (keywordAssignmentid, articleid, keywordid, createdat) 
                        VALUES ((SELECT COALESCE(MAX(keywordassignmentid), 0) + 1 FROM keywordAssignment), ?, ?, CURRENT_TIMESTAMP)
                    """;
                    jdbcTemplate.update(insertKeywordAssignmentSql, newArticleId, keywordId);
                }
                System.out.println("Keyword Assignments Inserted.");
            }

            // Set logged-in user attribute
            if (this.userExist != null && !this.userExist.isEmpty()) {
                model.addAttribute("loggedInUser", userExist); // Add the logged-in username
            } else {
                model.addAttribute("loggedInUser", null); // No user logged in
            }

        } catch (Exception e) {
            model.addAttribute("error", "Error while creating post: " + e.getMessage());
            e.printStackTrace();
            return "createPost";
        }
        
        return "redirect:/data";
    }	*/

    
    @GetMapping("/load-more-post")
    @ResponseBody
    public Map<String, Object> loadMorePost(@RequestParam("page") int page) {
        int pageSize = 5; // Number of posts per page
        int offset = (page - 1) * pageSize;

        // SQL query to fetch posts with pagination
        String postSql = """
                SELECT p.articleid, 
                       p.title, 
                       p.description, 
                       p.likes, 
                       p.dislikes, 
                       p.viewscount, 
                       p.commentscount AS comments, 
                       p.updatedat, 
                       p.postmedia AS image,
                       u.name AS name, 
                       u.username AS username, 
                       u.bio AS bio, 
                       c.name AS category
                FROM Post p 
                JOIN Blogger u ON p.primaryAuthor = u.authorid 
                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
                JOIN Category c ON pca.categoryid = c.categoryid
                ORDER BY p.updatedat DESC
                LIMIT ? OFFSET ?
            """;

        List<Map<String, Object>> posts = jdbcTemplate.query(postSql, (rs, rowNum) -> {
            Map<String, Object> post = new HashMap<>();
            int articleId = rs.getInt("articleid");

            post.put("articleid", articleId);
            post.put("title", rs.getString("title"));
            post.put("description", rs.getString("description"));
            post.put("likes", rs.getInt("likes"));
            post.put("dislikes", rs.getInt("dislikes"));
            post.put("viewscount", rs.getInt("viewscount"));
            post.put("comments", rs.getInt("comments"));
            post.put("updatedat", rs.getTimestamp("updatedat"));
            post.put("name", rs.getString("name"));
            post.put("username", rs.getString("username"));
            post.put("bio", rs.getString("bio"));
            post.put("category", rs.getString("category"));
            post.put("image", rs.getString("image"));

            // Fetch keywords for the current article
            String keywordQuery = """
                SELECT name 
                FROM Keyword k 
                JOIN KeywordAssignment ka ON k.keywordid = ka.keywordid 
                WHERE ka.articleid = ?
            """;
            List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, articleId);
            post.put("keywords", keywords);

            return post;
        }, pageSize, offset);

        // Check if there are more posts to load
        boolean hasMore = posts.size() == pageSize;

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("posts", posts);
        response.put("hasMore", hasMore);

        return response;
    }

    @GetMapping("/truncate")
    public String truncateTables(){
    	
    	List<String> tables = jdbcTemplate.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

    	for (String table : tables) {
            // Drop table if exists
            jdbcTemplate.execute("TRUNCATE TABLE " + table + " CASCADE;");
            System.out.println("Table " + table + " has been dropped.");
        }
    	
    	return "show";
    }

    
    @GetMapping("/destroy")
    public String destroyTables(){

    	try {
            // Get all table names in the database
            List<String> tables = jdbcTemplate.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

            // Loop through each table and drop it
            for (String table : tables) {
                // Drop table if exists
                jdbcTemplate.execute("DROP TABLE IF EXISTS " + table + " CASCADE;");
                System.out.println("Table " + table + " has been dropped.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    	
        jdbcTemplate.execute("DROP TYPE IF EXISTS blogger_status_enum;");
        jdbcTemplate.execute("DROP TYPE IF EXISTS post_status_enum;");
        jdbcTemplate.execute("DROP TYPE IF EXISTS post_comment_enum;");
        jdbcTemplate.execute("DROP TYPE IF EXISTS feedback_type_enum;");
        jdbcTemplate.execute("DROP TYPE IF EXISTS feedback_status_enum;");
        jdbcTemplate.execute("DROP TYPE IF EXISTS connection_status_enum;");
        jdbcTemplate.execute("DROP TYPE IF EXISTS reaction_enum;");
        jdbcTemplate.execute("DROP TYPE IF EXISTS membership_role_enum;");
        jdbcTemplate.execute("DROP TYPE IF EXISTS collaboration_role_enum;");
    	
    	return "show";
    }    
    
    @GetMapping("/insert_data")
    public String insertIntoTables() {
    
        // Insert bloggers
//    	String insertBloggers = """
//    		    INSERT INTO blogger (username, name, email, password, created_at, updated_at) VALUES
//    		    ('krish123', 'KRISH', 'krish@example.com', 'password1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
//    		    ('dhruv123', 'DHRUV', 'dhruv@example.com', 'password2', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
//    		    ('rahil123', 'RAHIL', 'rahil@example.com', 'password3', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
//    		    ('vishal123', 'VISHAL', 'vishal@example.com', 'password4', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
//    		    ('mohit123', 'MOHIT', 'mohit@example.com', 'password5', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
//    		    """;
//        jdbcTemplate.update(insertBloggers);

    	
        // Insert post
    	String insertpost = "INSERT INTO post (title, description, likes, dislikes, comments_count, primary_author, views_count, post_image, published_at, created_at, updated_at) VALUES\n" +
    		    "('First Blog Post', 'This is a description of the first blog post.', 0, 0, 0, 38, 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),\n" +
    		    "('Second Blog Post', 'This is a description of the second blog post.', 0, 0, 0,38, 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),\n" +
    		    "('Third Blog Post', 'This is a description of the third blog post.', 0, 0, 0, 38, 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),\n" +
    		    "('Fourth Blog Post', 'This is a description of the fourth blog post.', 0, 0, 0, 38, 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),\n" +
    		    "('Fifth Blog Post', 'This is a description of the fifth blog post.', 0, 0, 0, 38, 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);";
    		jdbcTemplate.update(insertpost);
        
    	return "show";
    }
    
    
    @GetMapping("/enums")
    public String enums() {

    		jdbcTemplate.execute("""
                DO $$ BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'blogger_status_enum') THEN
                        CREATE TYPE blogger_status_enum AS ENUM ('active', 'inactive', 'banned');
                    END IF;
                END $$;
            """);

            jdbcTemplate.execute("""
                DO $$ BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'post_status_enum') THEN
                        CREATE TYPE post_status_enum AS ENUM ('draft', 'published', 'archived');
                    END IF;
                END $$;
            """);

            jdbcTemplate.execute("""
                DO $$ BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'post_comment_enum') THEN
                        CREATE TYPE post_comment_enum AS ENUM ('comment', 'reply');
                    END IF;
                END $$;
            """);
            
            jdbcTemplate.execute("""
                DO $$ BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'reaction_enum') THEN
                        CREATE TYPE reaction_enum AS ENUM ('like', 'dislike');
                    END IF;
                END $$;
            """);

            jdbcTemplate.execute("""
                DO $$ BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'connection_status_enum') THEN
                        CREATE TYPE connection_status_enum AS ENUM ('pending', 'accepted', 'rejected');
                    END IF;
                END $$;
            """);

            jdbcTemplate.execute("""
                DO $$ BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'feedback_type_enum') THEN
                        CREATE TYPE feedback_type_enum AS ENUM ('bug_report', 'feature_request', 'general');
                    END IF;
                END $$;
            """);

            jdbcTemplate.execute("""
                DO $$ BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'feedback_status_enum') THEN
                        CREATE TYPE feedback_status_enum AS ENUM ('open', 'resolved');
                    END IF;
                END $$;
            """);

            jdbcTemplate.execute("""
                DO $$ BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'membership_role_enum') THEN
                        CREATE TYPE membership_role_enum AS ENUM ('member', 'admin', 'moderator');
                    END IF;
                END $$;
            """);

            jdbcTemplate.execute("""
                DO $$ BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'collaboration_role_enum') THEN
                        CREATE TYPE collaboration_role_enum AS ENUM ('author', 'editor', 'contributor');
                    END IF;
                END $$;
            """);

            // Add ENUM columns to appropriate tables
            jdbcTemplate.execute("""
                ALTER TABLE Blogger ADD COLUMN IF NOT EXISTS bloggerStatus blogger_status_enum DEFAULT 'active';
            """);

            jdbcTemplate.execute("""
                ALTER TABLE Post ADD COLUMN IF NOT EXISTS postStatus post_status_enum DEFAULT 'draft';
            """);		
            
            jdbcTemplate.execute("""
                 ALTER TABLE PostComment ADD COLUMN IF NOT EXISTS commentType post_comment_enum DEFAULT 'comment';
            """);	

            jdbcTemplate.execute("""
                ALTER TABLE PostInteraction ADD COLUMN IF NOT EXISTS reactionType reaction_enum;
            """);

            jdbcTemplate.execute("""
                ALTER TABLE Suggestion ADD COLUMN IF NOT EXISTS feedbackType feedback_type_enum;
            """);

            jdbcTemplate.execute("""
                ALTER TABLE Suggestion ADD COLUMN IF NOT EXISTS feedbackStatus feedback_status_enum DEFAULT 'open';
            """);

            jdbcTemplate.execute("""
                ALTER TABLE Membership ADD COLUMN IF NOT EXISTS membershipRole membership_role_enum DEFAULT 'member';
            """);

            jdbcTemplate.execute("""
                ALTER TABLE Collaboration ADD COLUMN IF NOT EXISTS colloborationRole collaboration_role_enum DEFAULT 'author';
            """);

            jdbcTemplate.execute("""
                ALTER TABLE Connection ADD COLUMN IF NOT EXISTS connectionStatus connection_status_enum DEFAULT 'pending';
            """);	

            // Add JSON columns to appropriate tables
            jdbcTemplate.execute("""
                ALTER TABLE Blogger ADD COLUMN IF NOT EXISTS socialLinks JSON;
            """);
            
            
            // Create a trigger function for automatically updating `updated_at`
           jdbcTemplate.execute("""
               CREATE OR REPLACE FUNCTION update_updated_at_column()
               RETURNS TRIGGER AS $$
               BEGIN
                   NEW.updatedAt = CURRENT_TIMESTAMP;
                   RETURN NEW;
               END;
               $$ LANGUAGE plpgsql;
           """);
            
            jdbcTemplate.execute("""
        	    DO $$
        	    BEGIN
        	        IF NOT EXISTS (
        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_blogger'
        	        ) THEN
        	            CREATE TRIGGER set_updated_at_blogger
        	            BEFORE UPDATE ON Blogger
        	            FOR EACH ROW
        	            EXECUTE FUNCTION update_updated_at_column();
        	        END IF;

        	        IF NOT EXISTS (
        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_post'
        	        ) THEN
        	            CREATE TRIGGER set_updated_at_post
        	            BEFORE UPDATE ON Post
        	            FOR EACH ROW
        	            EXECUTE FUNCTION update_updated_at_column();
        	        END IF;

        	        IF NOT EXISTS (
        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_category'
        	        ) THEN
        	            CREATE TRIGGER set_updated_at_category
        	            BEFORE UPDATE ON Category
        	            FOR EACH ROW
        	            EXECUTE FUNCTION update_updated_at_column();
        	        END IF;
        	        
        	        IF NOT EXISTS (
        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_keyword'
        	        ) THEN
        	            CREATE TRIGGER set_updated_at_keyword
        	            BEFORE UPDATE ON Keyword
        	            FOR EACH ROW
        	            EXECUTE FUNCTION update_updated_at_column();
        	        END IF;

        	        IF NOT EXISTS (
        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_communities'
        	        ) THEN
        	            CREATE TRIGGER set_updated_at_communities
        	            BEFORE UPDATE ON Community
        	            FOR EACH ROW
        	            EXECUTE FUNCTION update_updated_at_column();
        	        END IF;

        	        IF NOT EXISTS (
        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_keyword_assignments'
        	        ) THEN
        	            CREATE TRIGGER set_updated_at_keyword_assignments
        	            BEFORE UPDATE ON KeywordAssignment
        	            FOR EACH ROW
        	            EXECUTE FUNCTION update_updated_at_column();
        	        END IF;

        	        IF NOT EXISTS (
        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_post_category_assignments'
        	        ) THEN
        	            CREATE TRIGGER set_updated_at_post_category_assignments
        	            BEFORE UPDATE ON PostCategoryAssignment
        	            FOR EACH ROW
        	            EXECUTE FUNCTION update_updated_at_column();
        	        END IF;

        	        IF NOT EXISTS (
        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_memberships'
        	        ) THEN
        	            CREATE TRIGGER set_updated_at_memberships
        	            BEFORE UPDATE ON Membership
        	            FOR EACH ROW
        	            EXECUTE FUNCTION update_updated_at_column();
        	        END IF;

        	        IF NOT EXISTS (
        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_collaboration'
        	        ) THEN
        	            CREATE TRIGGER set_updated_at_collaboration
        	            BEFORE UPDATE ON Collaboration
        	            FOR EACH ROW
        	            EXECUTE FUNCTION update_updated_at_column();
        	        END IF;
        	    END;
        	    $$;
        	""");
            
            // function for => follower != following
            
            jdbcTemplate.execute("""
        	    CREATE OR REPLACE FUNCTION check_follower_following_ids()
        	    RETURNS TRIGGER AS $$
        	    BEGIN
        	        -- Check if follower_id and following_id are the same
        	        IF NEW.followerid = NEW.followingid THEN
        	            RAISE EXCEPTION 'Follower ID and Following ID cannot be the same';
        	        END IF;
        	        RETURN NEW;
        	    END;
        	    $$ LANGUAGE plpgsql;
        	""");
            
            jdbcTemplate.execute("""
        	    DO $$
        	    BEGIN
        	        IF NOT EXISTS (
        	            SELECT 1 FROM pg_trigger WHERE tgname = 'check_follower_following_connection'
        	        ) THEN
        	            CREATE TRIGGER check_follower_following_connection
        	            BEFORE INSERT ON Connection
        	            FOR EACH ROW
        	            EXECUTE FUNCTION check_follower_following_ids();
        	        END IF;
        	    END;
        	    $$;
        	""");
            
            Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM Category
            """,Integer.class);
            
            if( count == 0 ) {
            	jdbcTemplate.execute("""
            			INSERT INTO Category (categoryid, createdat, updatedat, createdby, categorydescription, categoryicon, name)
            			VALUES 
            			(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Coding is a category in which can explore the posts and articles regarding coding, coding memes and code of variety of programming languages and code', 'coding.jpg', 'Coding'),
            			(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Technology is a category in which can explore the posts and articles regarding technology and current trends of technologies', 'technology.jpg', 'Technology'),
						(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Robotics is a category in which can explore the posts and articles regarding robotics and the advance research related works for automation and revolutionary robotics field', 'robotics.jpg', 'Robotics'),
						(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'News is a category in which can explore the posts and articles regarding the latest news regarding any information on category also it makes you more updated on industry', 'news.jpg', 'News');
            	""");
            }
            
            count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM Keyword
            """,Integer.class);
            
            if( count == 0 ) {
            	jdbcTemplate.execute("""
            			INSERT INTO Keyword (keywordid, createdat, updatedat, keyworddescription, keywordicon, name)
            			VALUES 
            			(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Python is general purpose and multiuse programming language', 'python.jpg', 'Python'),
            			(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'C++ is system level and game development programming language', 'cpp.jpg', 'C++'),
						(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Java is enterprise and backend programming language', 'java.jpg', 'Java'),
				(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Rust is general purpose fast in computation programming language', 'rust.jpg', 'Rust');
                 """);
            }
            

            /*/ Add triggers to tables with `updated_at` column
            jdbcTemplate.execute("""
                CREATE TRIGGER set_updated_at_blogger
                BEFORE UPDATE ON Blogger
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at_column();
            """);

            jdbcTemplate.execute("""
                CREATE TRIGGER set_updated_at_post
                BEFORE UPDATE ON Post
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at_column();
            """);

            jdbcTemplate.execute("""
                CREATE TRIGGER set_updated_at_category
                BEFORE UPDATE ON Category
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at_column();
            """);

            jdbcTemplate.execute("""
                CREATE TRIGGER set_updated_at_communities
                BEFORE UPDATE ON Community
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at_column();
            """);

            jdbcTemplate.execute("""
                CREATE TRIGGER set_updated_at_keyword_assignments
                BEFORE UPDATE ON KeywordAssignment
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at_column();
            """);

            jdbcTemplate.execute("""
                CREATE TRIGGER set_updated_at_post_category_assignments
                BEFORE UPDATE ON PostCategoryAssignment
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at_column();
            """);

            jdbcTemplate.execute("""
                CREATE TRIGGER set_updated_at_memberships
                BEFORE UPDATE ON Membership
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at_column();
            """);

            jdbcTemplate.execute("""
                CREATE TRIGGER set_updated_at_collaboration
                BEFORE UPDATE ON Collaboration
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at_column();
            """);	*/


            
            return "redirect:/";
            
    }
    
    @GetMapping("/xyz")
    public String xyz() {

        // Insert into Blogger
    	jdbcTemplate.execute("""
            INSERT INTO Blogger (authorid, createdat, updatedat, bloggerstatus, sociallinks, password, profilepicture, bio, username, email, name)
            VALUES (1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'active', '{"twitter": "https://twitter.com/user"}', 'password123', 'profile.png', 'Sample bio', 'user123', 'user@example.com', 'User'),
            (2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'active', '{"twitter": "https://twitter.com/user2"}', 'password1234', 'profile.png2', 'Sample bio2', 'user1234', 'user2@example.com', 'Name');
        """);

        // Insert into Post
        jdbcTemplate.execute("""
            INSERT INTO Post (articleid, commentscount, createdat, viewscount, primaryauthor, poststatus, dislikes, likes, publishedat, updatedat, description, postmedia, title)
            VALUES (1, 0, CURRENT_TIMESTAMP, 0, 1, 'draft', 0, 0, NULL, CURRENT_TIMESTAMP, 'Post Description', 'post_media.png', 'Post Title');
        """);
    	
        // Insert into Category
        jdbcTemplate.execute("""
            INSERT INTO Category (categoryid, createdat, updatedat, createdby, description, categoryicon, name)
            VALUES (1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Category Description', 'icon.png', 'Category Name');
        """);
        
        // Insert into Community
        jdbcTemplate.execute("""
            INSERT INTO Community (communityid, createdat, updatedat, createdby, communitydescription, communityicon, name)
            VALUES (1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 'Community Description', 'community_icon.png', 'Community Name');
        """);
        
        // Insert into Keyword
        jdbcTemplate.execute("""
            INSERT INTO Keyword (keywordid, createdat, keyworddescription, name)
            VALUES (1, CURRENT_TIMESTAMP, 'Keyword Description', 'Keyword Name');
        """);

        // Insert into PostCategoryAssignment
        jdbcTemplate.execute("""
            INSERT INTO PostCategoryAssignment (postcategoryassignmentid, createdat, articleid, assignedby, categoryid)
            VALUES (1, CURRENT_TIMESTAMP, 1, 1, 1);
        """);

        // Insert into PostInteraction
        jdbcTemplate.execute("""
            INSERT INTO PostInteraction (postreactionid, createdat, articleid, authorid, reactiontype)
            VALUES (1, CURRENT_TIMESTAMP, 1, 1, 'like');
        """);
        
        // Insert into PostComment
        jdbcTemplate.execute("""
            INSERT INTO PostComment (postcommentid, articleid, commenttype, createdat, authorid, parentcommentid, comment)
            VALUES (1, 1, 'comment', CURRENT_TIMESTAMP, 1, NULL, 'This is a comment.');
        """);

        // Insert into KeywordAssignment
        jdbcTemplate.execute("""
            INSERT INTO KeywordAssignment (keywordassignmentid, createdat, articleid, assignedby, keywordid)
            VALUES (1, CURRENT_TIMESTAMP, 1, 1, 1);
        """);

        // Insert into Collaboration
        jdbcTemplate.execute("""
            INSERT INTO Collaboration (collaborationid, createdat, articleid, authorid, colloborationrole)
            VALUES (1, CURRENT_TIMESTAMP, 1, 1, 'author');
        """);
        
        // Insert into Membership
        jdbcTemplate.execute("""
            INSERT INTO Membership (membershipid, joinedat, authorid, communityid, membershiprole)
            VALUES (1, CURRENT_TIMESTAMP, 1, 1, 'member');
        """);

        // Insert into Suggestion
        jdbcTemplate.execute("""
            INSERT INTO Suggestion (suggestionid, createdat, authorid, feedbacktype, feedbackstatus, message)
            VALUES (1, CURRENT_TIMESTAMP, 1, 'feature_request', 'open', 'This is a feedback message.');
        """);
        
        // Insert into Connection
        jdbcTemplate.execute("""
            INSERT INTO Connection (connectionid, createdat, followerid, followingid, connectionstatus)
            VALUES (1, CURRENT_TIMESTAMP, 1, 2, 'pending');
        """);
    	
    	return "bloggers";
    }
    
    
    
    @GetMapping("/description")
    public String describeDatabase() {
    	String getDatabasesQuery = "SELECT datname FROM pg_database WHERE datistemplate = false;";
        List<String> databases = jdbcTemplate.queryForList(getDatabasesQuery, String.class);

        System.out.println("Available Databases:");
        databases.forEach(System.out::println);

        // Query to get all tables in the current database
        String getTablesQuery = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public';
                """;
        List<String> tables = jdbcTemplate.queryForList(getTablesQuery, String.class);

        System.out.println("\nTables and Their Columns:");

        // Loop through tables and get their column details
        for (String table : tables) {
            System.out.println("\nTable: " + table);
            String getColumnsQuery = String.format("""
                    SELECT column_name, data_type
                    FROM information_schema.columns
                    WHERE table_name = '%s';
                    """, table);

            List<Map<String, Object>> columns = jdbcTemplate.queryForList(getColumnsQuery);

            for (Map<String, Object> column : columns) {
                System.out.println("Column: " + column.get("column_name") + ", Data Type: " + column.get("data_type"));
            }
        }
        return "show";
    }
    
    @GetMapping("/data")
    public String dataOfDatabase() {
    	String getDatabasesQuery = "SELECT datname FROM pg_database WHERE datistemplate = false;";
        List<String> databases = jdbcTemplate.queryForList(getDatabasesQuery, String.class);

        System.out.println("Available Databases:");
        databases.forEach(System.out::println);

        // Query to get all tables in the current database
        String getTablesQuery = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public';
                """;
        List<String> tables = jdbcTemplate.queryForList(getTablesQuery, String.class);

        System.out.println("\nTables and Their Columns:");

        // Loop through tables and get their column details
        for (String table : tables) {
            System.out.println("\nTable: " + table);
            String getColumnsQuery = String.format("""
                    SELECT *
                    FROM %s;
                    """, table);

            List<Map<String, Object>> columns = jdbcTemplate.queryForList(getColumnsQuery);

            for (Map<String, Object> column : columns) {
                System.out.println("Row: " + column );
            }
        }
        return "show";
    }
    
    @PostMapping("/register")
    public String register(Model model, String name, String username, String email, String password, String confirmPassword, String bio, HttpServletRequest request) {

        // Check if the user already exists (by email or username)
        String checkUserSql = "SELECT COUNT(*) FROM Blogger WHERE email = ? OR username = ?";
        Integer existingUserCount = jdbcTemplate.queryForObject(checkUserSql, Integer.class, email, username);

        if (existingUserCount != null && existingUserCount > 0) {
            model.addAttribute("error", "Username or Email already exists!");
            return "register"; // Return to registration page with error message
        }
        
        if (this.userExist != null && !this.userExist.isEmpty()) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match!");
            return "register";
        }

        // Secure password hashing using PasswordEncoder
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(password);

        // Generate a new author ID
        String authorIdSql = "SELECT COALESCE(MAX(authorId) + 1, 1) FROM Blogger";
        Integer newAuthorId = jdbcTemplate.queryForObject(authorIdSql, Integer.class);

        // Insert the new user into the database
        String insertUserSql = """
            INSERT INTO Blogger (authorId, name, username, email, password, bio, profilePicture, createdAt, updatedAt) 
            VALUES (?, ?, ?, ?, ?, ?, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;

        int rowsAffected = jdbcTemplate.update(insertUserSql, newAuthorId, name, username, email, hashedPassword, bio);

        if (rowsAffected > 0) {

            request.getSession().setAttribute("authorId", newAuthorId);
        	
            return "redirect:/register?success=true"; // Redirect to login with success flag
        } else {
            model.addAttribute("error", "Registration failed. Please try again.");
            return "register";
        }
    }

    
    @PostMapping("/contact")
    public String submitSuggestion(HttpServletRequest request, Model model, String message) {
    	
        String suggestionId = "SELECT COALESCE(MAX(suggestionId) + 1, 1) FROM Blogger";
        Integer newSuggestionId = jdbcTemplate.queryForObject(suggestionId, Integer.class);

        if( (Integer) request.getSession().getAttribute("authorId") == null ) {
        	System.out.print(request.getSession().toString());
        	return "redirect:/login";
        }
        
        // Insert the new user into the database
        String insertUserSql = """
            INSERT INTO Blogger (suggestionId, authorId, message, createdAt ) 
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
        """;
        
        if (this.userExist != null && !this.userExist.isEmpty()) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }

        int rowsAffected = jdbcTemplate.update(insertUserSql, newSuggestionId, (Integer) request.getSession().getAttribute("authorId") , message);

    	return "redirect:/";
    }
    
    @GetMapping("/mass-filter")
    public String hideMassFilterPage(Model model) {
    
        String categorySql = "SELECT name FROM Category";
        List<String> categoriesForTags = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("categories", categoriesForTags);

        // Fetch keywords
        String keywordSql = "SELECT name FROM Keyword";
        List<String> keywordsForTags = jdbcTemplate.queryForList(keywordSql, String.class);
        model.addAttribute("keywords", keywordsForTags);
        
    	List<String> colors = new ArrayList<>(
    			List.of(
    				"purple", "cyan",  "green", "red", "blue", "black", "aliceblue", "yellow", "brown", "lightgreen", "lightblue", "pink"
    				)
    		);
    	
        if (this.userExist != null && !this.userExist.isEmpty()) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        	
        	model.addAttribute("topic", null ); 
        	model.addAttribute("colors", colors); 
        	model.addAttribute("news","Select The Category or Keyword Please");
            
            return "massfilter";
    }
    
    @GetMapping("/logout")
    public String logout() {
    	return "logout";
    }	
    
    @PostMapping("/custom-logout")
    public String destroySession(HttpServletRequest request, HttpServletResponse response, Authentication authentication, Model model, Principal principal) {
    	
    	this.userExist = "";
    	model.addAttribute("loggedInUser", null); // No user logged in
    	request.getSession().setAttribute("authorId", null);
    	
    	if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
    	
    	
    	return "redirect:/logout?success=true";
    }
    
    @GetMapping("/mass-filter/{entity}/{target}")
    public String showMassFilterPage(Model model, @PathVariable String entity, @PathVariable String target ) {
        // Fetch categories
        String categorySql = "SELECT name FROM Category";
        List<String> categoriesForTags = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("categories", categoriesForTags);

        // Fetch keywords
        String keywordSql = "SELECT name FROM Keyword";
        List<String> keywordsForTags = jdbcTemplate.queryForList(keywordSql, String.class);
        model.addAttribute("keywords", keywordsForTags);
        
        if( entity == null || target == null ) {
        	return "redirect:/mass-filter";
        }
        
        if (this.userExist != null && !this.userExist.isEmpty()) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
        entity = "%" + entity + "%";
        target = "%" + target+ "%";

        String sql;
        List<Map<String,Object>> posts = null;
        
        if( entity.equals("%category%") ) {
        	
        	sql = "SELECT categoryId FROM Category WHERE LOWER(name) LIKE LOWER(?)";
            List<Integer> categories = jdbcTemplate.queryForList(sql, Integer.class, "%" + target + "%");

            if (categories.isEmpty()) {
                model.addAttribute("error", "No posts found in this category.");
                model.addAttribute("posts", null);
                return "filter"; 
            }

            int categoryId = categories.get(0);

            // Query to retrieve posts along with keywords
            sql = """
                    SELECT p.articleid, 
                    p.title, 
                    p.description, 
                    p.likes, 
                    p.dislikes, 
                    p.viewscount, 
                    p.commentscount AS comments, 
                    p.updatedat, 
                    p.postmedia AS image,
                    u.name AS name, 
                    u.username AS username, 
                    u.bio AS bio, 
                    c.name AS category
	             FROM Post p 
	             JOIN Blogger u ON p.primaryAuthor = u.authorid 
	             JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
	             JOIN Category c ON pca.categoryid = c.categoryid
	             WHERE c.categoryid = ? LIMIT 5
	         """;

            posts = jdbcTemplate.query(sql, (rs, rowNum) -> {
	             Map<String, Object> post = new HashMap<>();
	             int articleId = rs.getInt("articleid");
	             
	             post.put("articleid", articleId);
	             post.put("title", rs.getString("title"));
	             post.put("description", rs.getString("description"));
	             post.put("likes", rs.getInt("likes"));
	             post.put("dislikes", rs.getInt("dislikes"));
	             post.put("viewscount", rs.getInt("viewscount"));
	             post.put("comments", rs.getInt("comments"));
	             post.put("updatedat", rs.getTimestamp("updatedat"));
	             post.put("name", rs.getString("name"));
	             post.put("username", rs.getString("username"));
	             post.put("bio", rs.getString("bio"));
	             post.put("category", rs.getString("category"));
	             post.put("image", rs.getString("image"));
	
	             // Separate query to get keywords for the current article
	             String keywordQuery = """
	             		SELECT name FROM Keyword k 
	             		JOIN KeywordAssignment ka 
	             		ON k.keywordid = ka.keywordid 
	             		WHERE ka.articleid = ?
	             	""";
	         	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Integer) articleId);
	         	post.put("keywords", keywords);
	
	             return post;
	         }, categoryId);
       	
        }
        
        if( entity.equals("%keyword%") ) {
        	
        	sql = "SELECT keywordId FROM Keyword WHERE LOWER(name) LIKE LOWER(?)";
            List<Integer> keywords = jdbcTemplate.queryForList(sql, Integer.class, "%" + target + "%");

            if (keywords.isEmpty()) {
                model.addAttribute("error", "No posts found in this category.");
                return "filter"; // Return with error message if no category found
            }

            int keywordId = keywords.get(0);

            // Fetch posts based on keyword ID
            sql = """
                SELECT 
                    p.articleid, 
                    p.title, 
                    p.description, 
                    p.likes, 
                    p.dislikes, 
                    p.viewscount, 
                    p.commentscount AS comments, 
                    p.updatedat, 
                    p.postmedia AS image,
                    u.name AS name, 
                    u.username AS username, 
                    u.bio AS bio, 
                    c.name AS category,
                    STRING_AGG(k.name, ',') AS keywords
	                FROM Post p 
	                JOIN Blogger u ON p.primaryAuthor = u.authorid 
	                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
	                JOIN Category c ON pca.categoryid = c.categoryid 
	                LEFT JOIN keywordAssignment pka ON p.articleid = pka.articleid 
	                LEFT JOIN Keyword k ON pka.keywordid = k.keywordid
	                WHERE k.keywordid = ?
	                GROUP BY p.articleid, p.title, p.description, p.likes, p.dislikes, 
	                         p.viewscount, p.commentscount, p.updatedat, p.postmedia,
	                         u.name, u.username, u.bio, c.name
	          """;

            posts = jdbcTemplate.query(sql, (rs, rowNum) -> {
            	Map<String, Object> post = new HashMap<>();
                int articleId = rs.getInt("articleid");
                
                post.put("articleid", articleId);
                post.put("title", rs.getString("title"));
                post.put("description", rs.getString("description"));
                post.put("likes", rs.getInt("likes"));
                post.put("dislikes", rs.getInt("dislikes"));
                post.put("viewscount", rs.getInt("viewscount"));
                post.put("comments", rs.getInt("comments"));
                post.put("updatedat", rs.getTimestamp("updatedat"));
                post.put("name", rs.getString("name"));
                post.put("username", rs.getString("username"));
                post.put("bio", rs.getString("bio"));
                post.put("category", rs.getString("category"));
                post.put("image", rs.getString("image"));

                // Separate query to get keywords for the current article
                String keywordQuery = """
                		SELECT name FROM Keyword k 
                		JOIN KeywordAssignment ka 
                		ON k.keywordid = ka.keywordid 
                		WHERE ka.articleid = ?
                	""";
            	List<String> keywordsOfPost = jdbcTemplate.queryForList(keywordQuery, String.class, (Integer) articleId);
            	post.put("keywords", keywordsOfPost);
            	
            	return post;
            } ,keywordId);
        	
        }

        model.addAttribute("posts", posts);
        
    	List<String> colors = new ArrayList<>(
			List.of(
				"purple", "cyan",  "green", "red", "blue", "black", "aliceblue", "yellow", "brown", "lightgreen", "lightblue", "pink"
				)
		);
    	
    	model.addAttribute("topic", target.substring(1,target.length() - 1) ); 
    	model.addAttribute("colors", colors); 
        
        return "massfilter";
    }
    
    @PostMapping("/profile")
    public ResponseEntity<Map<String, String>> update(@RequestBody String profileData) {
    	
    	String[] data = profileData.split("\":\"");
  
    	boolean success = true;
    	
    	
    	

        System.out.println("Received profile data: " + data[0] + " " +  data[1]);
    	
        Map<String, String> responseMap = new HashMap<>();
    	if (success) {
    	    responseMap.put(data[0], data[1]);  // Assuming data[0] and data[1] are valid keys and values
    	    return ResponseEntity.ok(responseMap);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    
}
