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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.Principal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@Controller
public class JdbcController {

	@Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HttpSession session;

    private static final ResourceUrlProvider resourceUrlProvider = new ResourceUrlProvider();
   
    
    Dotenv dotenv = Dotenv.load();
    
    private String userExist = "";
    private static Long start = 1L;

    private String searchKeyword = "technology";

    private String postStoreDirectory = dotenv.get("POST_STORE_DIRECTORY");
    private String bloggerStoreDirectory = dotenv.get("BLOGGER_STORE_DIRECTORY");

    private String postRetrieveDirectory = dotenv.get("POST_RETRIEVE_DIRECTORY");
    private String bloggerRetrieveDirectory = dotenv.get("BLOGGER_RETRIEVE_DIRECTORY");
    
    
    public String capitalize(String target) {
    	return target.substring(0,1).toUpperCase() + target.substring(1).toLowerCase() ;
    }
    
    @GetMapping("/secret")
    public String secret( HttpServletRequest request, Principal headPrincipal, Model model) {
    	
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
		            	request.getSession().setAttribute("admin", "admin");
			            return "redirect:/admin";  
		            }
		            
		            String sql = "SELECT authorId FROM Blogger WHERE username = ?";
		            Long authorId = jdbcTemplate.queryForObject(sql, Long.class, username );

		            if ( headPrincipal != null && userExist != null) {
		                model.addAttribute("loggedInUser", userExist = headPrincipal.getName()); // Add the logged-in username
		            } else {
		                model.addAttribute("loggedInUser", null); // No user logged in
		            }
		            
		            if (this.userExist != null && !this.userExist.isEmpty()) {
		                model.addAttribute("loggedInUser", userExist);
		            } else {
		                model.addAttribute("loggedInUser", null);
		            }
		            
		            request.getSession().setAttribute("authorId", authorId);
		            
		            System.out.print("ID : " + (Long) request.getSession().getAttribute("authorId") );
				   		            
		            return "redirect:/login?success=true";  
		        }		    
		    }
		}

		return "redirect:/login?error=false";
    }
    
    @GetMapping("/") // Maps to the root URL (http://localhost:8080/)
    public String home(Model model, Principal principal, HttpServletRequest request) {
        
    	if (principal != null && userExist != null) {
            model.addAttribute("loggedInUser", userExist = principal.getName()); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
    	String categorySql = "SELECT name FROM Category";
        List<String> categories = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("topics", categories);

        Long authorId = (Long) request.getSession().getAttribute("authorId");
        
        if( authorId != null && authorId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		model.addAttribute("personalImage",null);
        	}else {
        		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	model.addAttribute("personalImage",null);
        }
        
        
        String postSql = """
                SELECT p.articleid, 
                       p.primaryAuthor AS author, 
                       p.title, 
                       p.description, 
                       p.likes, 
                       p.dislikes, 
                       p.viewscount, 
                       p.commentscount AS comments, 
                       p.updatedat, 
                       p.postmedia AS media,
                    p.poststatus AS status,
                       u.name AS name, 
                       u.username AS username, 
                       u.bio AS bio,
                       u.profilePicture AS image, 
                       c.name AS category
                FROM Post p 
                JOIN Blogger u ON p.primaryAuthor = u.authorid 
                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
                JOIN Category c ON pca.categoryid = c.categoryid 
                ORDER BY p.createdat DESC
                LIMIT 5
            """;

        	// Long authorId = (Long) request.getSession().getAttribute("authorId");
        
            List<Map<String, Object>> posts = jdbcTemplate.query(postSql, (rs, rowNum) -> {
                Map<String, Object> post = new HashMap<>();
                Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
                
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
                post.put("title", rs.getString("title"));
                post.put("disable",false);
                post.put("description", rs.getString("description"));
                post.put("likes", rs.getInt("likes"));
                post.put("dislikes", rs.getInt("dislikes"));
                post.put("viewscount", rs.getInt("viewscount"));
                post.put("comments", rs.getInt("comments"));
                Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
                post.put("name", rs.getString("name"));
                post.put("username", rs.getString("username"));
                post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
                post.put("category", rs.getString("category"));
                if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
                if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

                // Separate query to get keywords for the current article
                String keywordQuery = """
                		SELECT name FROM Keyword k 
                		JOIN KeywordAssignment ka 
                		ON k.keywordid = ka.keywordid 
                		WHERE ka.articleid = ?
                	""";
            	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
            	post.put("keywords", keywords);

            	
            	if( authorId != null && authorId > 0 ) {

            		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
            		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
	            	post.put("isLiked", isReact == 0 ? false : true);
	            	
	            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
	            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	            	
	            	post.put("isDisliked", isReact == 0 ? false : true );
            	
            	}else {
            		post.put("isLiked", false );            		
            		post.put("isDisliked", false );
            	}
	            	
                return post;
            });

        model.addAttribute("posts", posts);

        System.out.print(posts);

    	List<String> colors = new ArrayList<>(
    			List.of(
                    "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                    "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                    "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                    "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                    "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                    "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                    "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                    "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                    "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"
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
                       p.primaryAuthor AS author, 
                       p.title, 
                       p.description, 
                       p.likes, 
                       p.dislikes, 
                       p.viewscount, 
                       p.commentscount AS comments, 
                       p.updatedat, 
                       p.postmedia AS media,
                    p.poststatus AS status,
                       u.name AS name, 
                       u.username AS username, 
                       u.bio AS bio, 
                       u.profilepicture AS image,
                       c.name AS category
                FROM Post p 
                JOIN Blogger u ON p.primaryAuthor = u.authorid 
                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
                JOIN Category c ON pca.categoryid = c.categoryid
                WHERE u.authorid = ? ORDER BY p.createdat DESC
            """;
        
        	Long authorId = (Long) request.getSession().getAttribute("authorId");

        	if( authorId != null && authorId > 0 ) {
            	
            	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
            	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
            	
            	if( imageSQL == null || imageSQL.equals("") ) {
            		model.addAttribute("personalImage",null);
            	}else {
            		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
            	}
            	
            	
            }else {
            	model.addAttribute("personalImage",null);
            }
        	
            List<Map<String, Object>> posts = jdbcTemplate.query(postSql, (rs, rowNum) -> {
                Map<String, Object> post = new HashMap<>();
                Long articleId = (Long) rs.getLong("articleid");
                
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
                post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
                post.put("title", rs.getString("title"));
                post.put("disable",false);
                post.put("description", rs.getString("description"));
                post.put("likes", rs.getInt("likes"));
                post.put("dislikes", rs.getInt("dislikes"));
                post.put("viewscount", rs.getInt("viewscount"));
                post.put("comments", rs.getInt("comments"));
                Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
                post.put("name", rs.getString("name"));
                post.put("username", rs.getString("username"));
                post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
                post.put("category", rs.getString("category"));
                if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
                if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

                // Separate query to get keywords for the current article
                String keywordQuery = """
                		SELECT name FROM Keyword k 
                		JOIN KeywordAssignment ka 
                		ON k.keywordid = ka.keywordid 
                		WHERE ka.articleid = ?
                	""";
            	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
            	post.put("keywords", keywords);

            	if( authorId != null && authorId > 0 ) {

            		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
            		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
	            	post.put("isLiked", isReact == 0 ? false : true);
	            	
	            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
	            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	            	
	            	post.put("isDisliked", isReact == 0 ? false : true );
            	
            	}else {
            		post.put("isLiked", false );            		
            		post.put("isDisliked", false );
            	}
            	
                return post;
            }, authorId);

        model.addAttribute("posts", posts);

        System.out.print(posts);

    	List<String> colors = new ArrayList<>(
    			List.of(
                    "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                    "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                    "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                    "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                    "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                    "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                    "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                    "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                    "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"
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
    public String getAllBloggers(Model model, HttpServletRequest request) {
    	
    	Long authorId = (Long) request.getSession().getAttribute("authorId");
    	
        String sql = "SELECT authorId,name,username,profilePicture AS image,createdat, bio FROM Blogger ORDER BY authorId ASC ";
        List<Map<String, Object>> bloggers = jdbcTemplate.queryForList(sql);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");

        for (Map<String, Object> blogger : bloggers) {
            Timestamp createdat = (Timestamp) blogger.get("createdat");
            
//            System.out.println("Blogger Is " + blogger );
            Long author = (blogger.get("authorid") != null) ? (Long) blogger.get("authorId") : null;
            
            try {
            
            	// my id is in followers that means 
            	// that means i am follower of someone 
            	// that is following for me
            	
	            sql = """
	            		SELECT COUNT(*) AS followings FROM Connection WHERE followerId = 
	            		""" + author;
	            blogger.put("followings", jdbcTemplate.queryForObject(sql, Long.class)	 );
	            
            }catch( Exception e ) {
            	e.printStackTrace();
            	System.out.print("\n\n1" + "\n\n");
            	blogger.put("followings", 0);
            }
	     
            try {
            	
            	// my id is in following that means 
            	// someone is my follower
            	
	            sql = """
	            		SELECT COUNT(*) AS followers FROM Connection WHERE followingId = 
	            		""" + author;
	            blogger.put("followers", jdbcTemplate.queryForObject(sql, Long.class) );
	
	        }catch( Exception e ) {
            	e.printStackTrace();
            	System.out.print("\n\n2" + "\n\n");
	        	blogger.put("followers", 0);
	        }
            
            try {
	            Long likes = 0L;
            	
	            sql = "SELECT articleId FROM Post WHERE primaryAuthor = ?";
	            List<Long> articles = jdbcTemplate.queryForList(sql, Long.class, author);
	            
	            for( Long articleId : articles ) {
	            
	            	sql = """
		            		SELECT COUNT(*) AS likes FROM PostInteraction WHERE articleId = 
		            	""" + articleId + " AND reactionType = 'like' ";
		            likes += jdbcTemplate.queryForObject(sql, Long.class);
	            
	            }
		            
	            blogger.put("likes", likes);
		
		    }catch( Exception e ) {
            	e.printStackTrace();
            	System.out.print("\n\n3" + "\n\n");
		    	blogger.put("likes", 0);
		    }   
            
            try {
            	
            	sql = "SELECT articleId FROM Post WHERE primaryAuthor = " + author;
            	List<Long> ids = jdbcTemplate.queryForList(sql, Long.class);
            	
            	Long comments = 0L;
            	
            	for( Long id : ids ) {
            		
            		sql = """
	            		SELECT commentscount FROM Post WHERE articleId = 
	            	""" +  id;
            		
            		comments += jdbcTemplate.queryForObject(sql, Long.class);
            		
            	}
	            blogger.put("comments", comments );
	            
			}catch( Exception e ) {
            	e.printStackTrace();
            	System.out.print("\n\n4" + "\n\n");
				blogger.put("comments", 0);
			}       
            
            try {
	            sql = """
	            		SELECT COUNT(*) AS posts FROM Post WHERE primaryAuthor =
	            		""" + author;
	            blogger.put("posts", jdbcTemplate.queryForObject(sql, Long.class) );
	            
	        }catch( Exception e ) {
            	e.printStackTrace();
            	System.out.print("\n\n5" + "\n\n");
	        	blogger.put("posts", 0);
	        }    
            
            try {
                sql = "SELECT COUNT(*) FROM Connection WHERE followerId = " + authorId + " AND followingId = " + author ;
                Long count = jdbcTemplate.queryForObject(sql, Long.class);
                System.out.println(sql);  // This should print the parameterized query
                blogger.put("status", count != null && count > 0 ); 
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("\n\nError while fetching connection status: " + e.getMessage() + "\n\n");
                blogger.put("status", false);
            }

            
            /* try {
                sql = "SELECT COUNT(*) FROM Connection WHERE followerId = " + authorId + " AND followingId = " + author ;
                Long count = jdbcTemplate.queryForObject(sql, Long.class);
                System.out.println(sql);
                blogger.put("status", count > 0 ? "true" : "false");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("\n\nError while fetching connection status: " + e.getMessage() + "\n\n");
                blogger.put("status", false);
            } */
            
            

            if( author == authorId || author == null ) {
            	blogger.put("status", null);            	
            }
            
            if (createdat != null) {
                blogger.put("createdat", sdf.format(createdat));
            }
            
            if( blogger.get("image") == null || blogger.get("image").equals("") ) {
            	blogger.put("image", null);
            }else {
            	blogger.put("image", bloggerRetrieveDirectory + blogger.get("image"));
            }
            
        }
        
        model.addAttribute("bloggers", bloggers);
        model.addAttribute("isMe", authorId);

        if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
        System.out.print(bloggers);
        
        return "bloggers";
    }
    
    // Show all post
    @GetMapping("/posts")
    public String showpost(Model model, HttpServletRequest request) {
    	
    	String categorySql = "SELECT name FROM Category";
        List<String> categories = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("topics", categories);

        String postSql = """
                SELECT p.articleid, 
                       p.primaryAuthor AS author, 
                       p.title, 
                       p.description, 
                       p.likes, 
                       p.dislikes, 
                       p.viewscount, 
                       p.commentscount AS comments, 
                       p.updatedat, 
                       p.postmedia AS media,
                    p.poststatus AS status,
                       u.name AS name, 
                       u.username AS username, 
                       u.bio AS bio, 
                       u.profilepicture AS image,
                       c.name AS category
                FROM Post p 
                JOIN Blogger u ON p.primaryAuthor = u.authorid 
                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
                JOIN Category c ON pca.categoryid = c.categoryid
            """;

        	Long authorId = (Long) request.getSession().getAttribute("authorId");
        
        	if( authorId != null && authorId > 0 ) {
            	
            	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
            	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
            	
            	if( imageSQL == null || imageSQL.equals("") ) {
            		model.addAttribute("personalImage",null);
            	}else {
            		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
            	}
            	
            	
            }else {
            	model.addAttribute("personalImage",null);
            }
        	
            List<Map<String, Object>> posts = jdbcTemplate.query(postSql, (rs, rowNum) -> {
                Map<String, Object> post = new HashMap<>();
                Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
                
                
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
                post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
                post.put("title", rs.getString("title"));
                post.put("disable",false);
                post.put("description", rs.getString("description"));
                post.put("likes", rs.getInt("likes"));
                post.put("dislikes", rs.getInt("dislikes"));
                post.put("viewscount", rs.getInt("viewscount"));
                post.put("comments", rs.getInt("comments"));
                Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
                post.put("name", rs.getString("name"));
                post.put("username", rs.getString("username"));
                post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
                post.put("category", rs.getString("category"));
                if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
                if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

                // Separate query to get keywords for the current article
                String keywordQuery = """
                		SELECT name FROM Keyword k 
                		JOIN KeywordAssignment ka 
                		ON k.keywordid = ka.keywordid 
                		WHERE ka.articleid = ?
                	""";
            	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
            	post.put("keywords", keywords);

            	if( authorId != null && authorId > 0 ) {

            		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
            		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
	            	post.put("isLiked", isReact == 0 ? false : true);
	            	
	            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
	            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	            	
	            	post.put("isDisliked", isReact == 0 ? false : true );
            	
            	}else {
            		post.put("isLiked", false );            		
            		post.put("isDisliked", false );
            	}
            	
                return post;
            });

        model.addAttribute("posts", posts);

        System.out.print(posts);

    	List<String> colors = new ArrayList<>(
    			List.of(
    				"red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                    "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                    "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                    "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                    "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                    "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                    "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                    "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                    "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"
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
    
    /* @PostMapping("/review")
    public String reviewPost(Model model, String title, String category, String description, String selectedKeywords, MultipartFile image, HttpServletRequest request ) {
        
    	String sql = "SELECT username,name,bio, FROM Blogger where authorId=?";
    	
    	Map<String,Object> user = jdbcTemplate.queryForMap(sql, (Long) request.getSession().getAttribute("authorId") );
    	
    	Map<String, Object> postData = new HashMap<>();
        postData.put("title", title);
        post.put("disable",false);
        postData.put("category", category);
        description = description.replaceAll("<[^>]*>", "").trim();
        postData.put("description", description);
        
        postData.put("username", user.get("username") );
        postData.put("name", user.get("name") );
        postData.put("bio", user.get("bio") );
        postData.put("image", user.get("image") );
        
        String uploadedImagePath = null;

        if (image!= null && !image.isEmpty()) {
            try {
                // Define the path to save the image
                String uploadDir = postStoreDirectory;
                String fileName = image.getOriginalFilename();
                
                // Ensure the directory exists
                File directory = new File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                
                Long authorId = (Long) request.getSession().getAttribute("authorId");
                // Save the file
                uploadedImagePath = String.valueOf( authorId ) + "-" + fileName;


                System.out.print("ID : " + String.valueOf(authorId));
                
                Path filePath = Paths.get(uploadDir, uploadedImagePath);
                Files.write(filePath, image.getBytes());

                System.out.print("File : " + uploadedImagePath);
                
                postData.put("image", uploadedImagePath);

            } catch (IOException e) {
                return "redirect:/create_post?error=true";
            }
        }

        
        String keyword = selectedKeywords;
        System.out.print("Keyword " + keyword);
        List<String> keywords = new ArrayList<>();
        List<Long> buttonIndex = new ArrayList<>();
        StringTokenizer tokens = new StringTokenizer(keyword, ",");

        keyword = "";
        
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            String[] splitToken = token.split("-");
            if (splitToken.length > 1) {
            	keywords.add(splitToken[0]);
            	keyword += splitToken[0] +  ",";// Add the token (keyword) to the keywords list
                buttonIndex.add(Long.valueOf(splitToken[1]));
            }
        }

        

        
        System.out.print(buttonIndex);
        
        // Add the list to the post map
        postData.put("keyword", keyword);
        postData.put("keywords", keywords);
        postData.put("buttonIndex", buttonIndex);

        List<String> colors = new ArrayList<>(
			List.of(
                "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"
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
    } 	*/
    
    @PostMapping("/review")
    public String reviewPost(Model model, String title, String category, String description, String selectedKeywords, MultipartFile image, HttpServletRequest request ) {
        
        Long primaryAuthorId = (Long) request.getSession().getAttribute("authorId");;  // Assuming logged-in user ID
        Long authorId = primaryAuthorId;
        
        Long newArticleId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(articleid), 0) + 1 FROM Post", Long.class);
        
        String uploadedImagePath = null;
        String media = "";

        if (image!= null && !image.isEmpty()) {
            try {
                // Define the path to save the image
                String uploadDir = postStoreDirectory;
                String fileName = image.getOriginalFilename();
                
                // Ensure the directory exists
                File directory = new File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                
                String[] parts = fileName.split("\\.");
                String extension = parts[parts.length - 1];

            	media = "post-" + newArticleId + "." + extension;
                uploadedImagePath = media;

                System.out.print("ID : " + String.valueOf(authorId));
                
                Path filePath = Paths.get(uploadDir, uploadedImagePath);
                Files.write(filePath, image.getBytes());

                System.out.print("File : " + uploadedImagePath);
                
            } catch (IOException e) {
                return "redirect:/create_post?error=true";
            }
        }
        
	    String insertPostSql = """
	            INSERT INTO Post (articleid, title, description, likes, dislikes, commentscount, primaryauthor, viewscount, postmedia, publishedat, createdat, updatedat) 
	            VALUES (?, ?, ?, 0, 0, 0, ?, 0, ? , CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
	        """;
	        
	        int postRowsAffected = jdbcTemplate.update(insertPostSql, newArticleId, title, description, primaryAuthorId, media);
	        System.out.println("Post Inserted. Rows affected: " + postRowsAffected);
	
	        // Step 3: Insert into PostCategoryAssignment
	        String getCategoryIdSql = "SELECT categoryid FROM Category WHERE LOWER(name) = LOWER(?)";
	        Long categoryId = null;
	        try {
	            categoryId = jdbcTemplate.queryForObject(getCategoryIdSql, Long.class, category);
	        } catch (EmptyResultDataAccessException e) {
	            System.out.println("Category not found: " + category);
	        }
	        
	        if (categoryId != null) {
	        	
	        	Long postCategoryAssignmnetId = jdbcTemplate.queryForObject( """
	        			SELECT COALESCE(MAX(postCategoryAssignmentId), 0) + 1 FROM PostCategoryAssignment
	        			""", Long.class);
	        	
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
	        String keyword = selectedKeywords;
	        System.out.print("Keyword " + keyword);
	        List<String> keywords = new ArrayList<>();

	        List<Long> buttonIndex = new ArrayList<>();
	        StringTokenizer tokens = new StringTokenizer(keyword, ",");

	        keyword = "";
	        
	        while (tokens.hasMoreTokens()) {
	            String token = tokens.nextToken();
	            String[] splitToken = token.split("-");
	            if (splitToken.length > 1) {
	            	keywords.add(splitToken[0]);
	            	keyword += splitToken[0] +  ",";// Add the token (keyword) to the keywords list
	                buttonIndex.add(Long.valueOf(splitToken[1]));
	            }
	        }
	        
	        if (keywords.size() > 0) {
	            for (String currentKeyword : keywords) {
	                // Get keywordId (we assume keywords are case-insensitive)
	                String getKeywordIdSql = "SELECT keywordId FROM Keyword WHERE LOWER(name) LIKE LOWER(?)";
	                Long keywordId = null;
	                try {
	                    keywordId = jdbcTemplate.queryForObject(getKeywordIdSql, Long.class, "%" + currentKeyword.split("-")[0] + "%");
	                } catch (EmptyResultDataAccessException e) {
	                    System.out.println("Keyword not found: " + currentKeyword);
	                }
	                
	                if (keywordId != null) {
	                	
	                	Long keywordAssignmentId = jdbcTemplate.queryForObject( """
	                			SELECT COALESCE(MAX(keywordAssignmentId), 0) + 1 FROM KeywordAssignment
	                		""", Long.class);
	                	
	                    String insertKeywordAssignmentSql = """
	                        INSERT INTO KeywordAssignment ( articleid, keywordid, assignedBy, createdat) 
	                        VALUES (?, ?, ?, CURRENT_TIMESTAMP)
	                    """;
	                    jdbcTemplate.update(insertKeywordAssignmentSql, newArticleId, keywordId, primaryAuthorId);
	                    System.out.println("Keyword Assignments Inserted." +  insertKeywordAssignmentSql);
	                }
	            }
	        }	
	        
	        String sql = """
	    	        SELECT p.articleid, 
                       p.primaryAuthor AS author, 
	    	               p.title, 
	    	               p.description, 
	    	               p.likes, 
	    	               p.dislikes, 
	    	               p.viewscount, 
	    	               p.commentscount AS comments, 
	    	               p.updatedat, 
	    	               p.postmedia AS media,
                    p.poststatus AS status,
	    	               u.name AS name, 
	    	               u.username AS username, 
	    	               u.bio AS bio, 
	    	               u.profilepicture AS image,
	    	               c.name AS category
	    	        FROM Post p 
	    	        JOIN Blogger u ON p.primaryAuthor = u.authorid 
	    	        JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
	    	        JOIN Category c ON pca.categoryid = c.categoryid
	                WHERE p.articleid = ?
	            """;
	            
	            List<Map<String, Object>> posts = jdbcTemplate.query(sql, (rs, rowNum) -> {
	    	            Map<String, Object> post = new HashMap<>();
	    	            Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
                
                
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
	    	            
	    	            post.put("articleid", articleId);
	    	            post.put("author", rs.getLong("author"));
	    	            post.put("title", rs.getString("title"));
	    	            post.put("disable",false);
	    	            post.put("description", rs.getString("description"));
	    	            post.put("likes", rs.getInt("likes"));
	    	            post.put("dislikes", rs.getInt("dislikes"));
	    	            post.put("viewscount", rs.getInt("viewscount"));
	    	            post.put("comments", rs.getInt("comments"));
	    	            Timestamp timestamp = rs.getTimestamp("updatedat");
	    	            if (timestamp != null) {
	    	                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
	    	                String formattedDate = sdf.format(timestamp);
	    	                post.put("updatedat", formattedDate);
	    	            } else {
	    	                post.put("updatedat", null);
	    	            }
	    	            post.put("name", rs.getString("name"));
	    	            post.put("username", rs.getString("username"));
	    	            post.put("bio", rs.getString("bio"));
	    	            post.put("status", rs.getString("status"));
	    	            post.put("category", rs.getString("category"));
	                    if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	    	post.put("media", null);
	    }else {
	        post.put("media", postRetrieveDirectory + rs.getString("media"));
	    }
	    	            if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	    	post.put("image", null);
	    }else {
	        post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
	    }
	    	
	    	            // Separate query to get keywords for the current article
	    	            String keywordQuery = """
	    	            		SELECT name FROM Keyword k 
	    	            		JOIN KeywordAssignment ka 
	    	            		ON k.keywordid = ka.keywordid 
	    	            		WHERE ka.articleid = ?
	    	            	""";
	    	        	List<String> Tkeywords = jdbcTemplate.queryForList(keywordQuery, String.class, newArticleId);
	    	        	post.put("keywords", Tkeywords);

	    	        	if( authorId != null && authorId > 0 ) {

	                		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
	                		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	                	
	    	            	post.put("isLiked", isReact == 0 ? false : true);
	    	            	
	    	            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
	    	            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	    	            	
	    	            	post.put("isDisliked", isReact == 0 ? false : true );
	                	
	                	}else {
	                		post.put("isLiked", false );            		
	                		post.put("isDisliked", false );
	                	}
	    	        	
	    	            post.put("buttonIndex", buttonIndex);
	    	        	model.addAttribute("title", post.get("title"));
	    	            return post;

	            }, newArticleId);

	        	model.addAttribute("title", newArticleId);	    	
	            List<String> colors = new ArrayList<>(
	        			List.of(
                            "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                            "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                            "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                            "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                            "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                            "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                            "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                            "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                            "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"
                        )
	        		);
	            
	            model.addAttribute("colors", colors);
	            model.addAttribute("posts", posts);
	        
	        if (this.userExist != "" && userExist != null ) {
	            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
	        } else {
	            model.addAttribute("loggedInUser", null); // No user logged in
	        }
	        
	        if( authorId != null && authorId > 0 ) {
	        	
	        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
	        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
	        	
	        	if( imageSQL == null || imageSQL.equals("") ) {
	        		model.addAttribute("personalImage",null);
	        	}else {
	        		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
	        	}
	        	
	        	
	        }else {
	        	model.addAttribute("personalImage",null);
	        }
	        
	        return "reviewPost";
	        
    }

    @GetMapping("/post/{id}")
    public String viewPost(Model model, @PathVariable Long id, HttpServletRequest request) {
    	
    	Long authorId = (Long) request.getSession().getAttribute("authorId");
    	
    	if( authorId != null && authorId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		model.addAttribute("personalImage",null);
        	}else {
        		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	model.addAttribute("personalImage",null);
        }
    	
    	String categorySql = "SELECT name FROM Category";
        List<String> categories = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("topics", categories);
    	
        String sql = """
	        SELECT p.articleid, 
                       p.primaryAuthor AS author, 
	               p.title, 
	               p.description, 
	               p.likes, 
	               p.dislikes, 
	               p.viewscount, 
	               p.commentscount AS comments, 
	               p.updatedat, 
	               p.postmedia AS media,
                    p.poststatus AS status,
	               u.name AS name, 
	               u.username AS username, 
	               u.bio AS bio, 
	               u.profilepicture AS image,
	               c.name AS category
	        FROM Post p 
	        JOIN Blogger u ON p.primaryAuthor = u.authorid 
	        JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
	        JOIN Category c ON pca.categoryid = c.categoryid
            WHERE p.articleid = ?
        """;
        
        List<Map<String, Object>> posts = jdbcTemplate.query(sql, (rs, rowNum) -> {
	            Map<String, Object> post = new HashMap<>();
	            Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
                
                
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
	            
	            post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
	            post.put("title", rs.getString("title"));
	            post.put("disable",false);
	            post.put("description", rs.getString("description"));
	            post.put("likes", rs.getInt("likes"));
	            post.put("dislikes", rs.getInt("dislikes"));
	            post.put("viewscount", rs.getInt("viewscount"));
	            post.put("comments", rs.getInt("comments"));
	            Timestamp timestamp = rs.getTimestamp("updatedat");
	            if (timestamp != null) {
	                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
	                String formattedDate = sdf.format(timestamp);
	                post.put("updatedat", formattedDate);
	            } else {
	                post.put("updatedat", null);
	            }
	            post.put("name", rs.getString("name"));
	            post.put("username", rs.getString("username"));
	            post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
	            post.put("category", rs.getString("category"));
                if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
	            if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}
	
	            // Separate query to get keywords for the current article
	            String keywordQuery = """
	            		SELECT name FROM Keyword k 
	            		JOIN KeywordAssignment ka 
	            		ON k.keywordid = ka.keywordid 
	            		WHERE ka.articleid = ?
	            	""";
	        	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
	        	post.put("keywords", keywords);
	
	        	if( authorId != null && authorId > 0 ) {

            		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
            		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
	            	post.put("isLiked", isReact == 0 ? false : true);
	            	
	            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
	            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	            	
	            	post.put("isDisliked", isReact == 0 ? false : true );
            	
            	}else {
            		post.put("isLiked", false );            		
            		post.put("isDisliked", false );
            	}
	        	
	        	model.addAttribute("title", post.get("title"));
	            return post;

        }, id);
	
	
        model.addAttribute("posts", posts);

        System.out.print(posts);
	
		List<String> colors = new ArrayList<>(
				List.of(
                    "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                    "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                    "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                    "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                    "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                    "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                    "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                    "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                    "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"
				)
			);
		
		model.addAttribute("colors", colors);
        
        if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
        return "viewPost"; // Assuming there's a Thymeleaf template named "viewPost"
    }

    
    
    @GetMapping("/filter/category/{category}")
    public String filterCategoryPost(Model model, @PathVariable String category, HttpServletRequest request) {
        // Fetch category ID based on name (case-insensitive)
        String sql = "SELECT categoryId FROM Category WHERE LOWER(name) LIKE LOWER(?)";
        List<Long> categories = jdbcTemplate.queryForList(sql, Long.class, "%" + category + "%");

        if (categories.isEmpty()) {
            model.addAttribute("error", "No posts found in this category.");
            model.addAttribute("posts", null);
            return "filter"; 
        }

        Long categoryId = categories.get(0);
        Long authorId = (Long) request.getSession().getAttribute("authorId");

        if( authorId != null && authorId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		model.addAttribute("personalImage",null);
        	}else {
        		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	model.addAttribute("personalImage",null);
        }
        
        // Query to retrieve posts along with keywords
        sql = """
                SELECT p.articleid, 
                       p.primaryAuthor AS author, 
                p.title, 
                p.description, 
                p.likes, 
                p.dislikes, 
                p.viewscount, 
                p.commentscount AS comments, 
                p.updatedat, 
                p.postmedia AS media,
                    p.poststatus AS status,
                u.name AS name, 
                u.username AS username, 
                u.bio AS bio, 
                u.profilepicture AS image,
                c.name AS category
         FROM Post p 
         JOIN Blogger u ON p.primaryAuthor = u.authorid 
         JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
         JOIN Category c ON pca.categoryid = c.categoryid
         WHERE c.categoryid = ? 
     """;

        List<String> colors = new ArrayList<>(
    			List.of(
                    "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                    "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                    "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                    "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                    "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                    "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                    "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                    "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                    "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"
    			)
    		);
    	model.addAttribute("colors", colors);
        
     List<Map<String, Object>> posts = jdbcTemplate.query(sql, (rs, rowNum) -> {
         Map<String, Object> post = new HashMap<>();
         Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
                
                
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
         
         post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
         post.put("title", rs.getString("title"));
         post.put("disable",false);
         post.put("description", rs.getString("description"));
         post.put("likes", rs.getInt("likes"));
         post.put("dislikes", rs.getInt("dislikes"));
         post.put("viewscount", rs.getInt("viewscount"));
         post.put("comments", rs.getInt("comments"));
         Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
         post.put("name", rs.getString("name"));
         post.put("username", rs.getString("username"));
         post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
         post.put("category", rs.getString("category"));
         if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
         if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

         // Separate query to get keywords for the current article
         String keywordQuery = """
         		SELECT name FROM Keyword k 
         		JOIN KeywordAssignment ka 
         		ON k.keywordid = ka.keywordid 
         		WHERE ka.articleid = ?
         	""";
     	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
     	post.put("keywords", keywords);

    	if( authorId != null && authorId > 0 ) {

    		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
    		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
    	
        	post.put("isLiked", isReact == 0 ? false : true);
        	
        	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
        	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
        	
        	post.put("isDisliked", isReact == 0 ? false : true );
    	
    	}else {
    		post.put("isLiked", false );            		
    		post.put("isDisliked", false );
    	}
     	
         return post;
     }, categoryId);

        model.addAttribute("posts", posts);
        model.addAttribute("topic",  this.capitalize(category) );
        model.addAttribute("loggedInUser", (userExist != null && !userExist.isEmpty()) ? userExist : null);

        return "filter"; 
    }
    
    
    @GetMapping("/filter/keyword/{keyword}")
    public String filterKeywordpost(Model model, @PathVariable String keyword, HttpServletRequest request) {
    	
        String sql = "SELECT keywordId FROM Keyword WHERE LOWER(name) LIKE LOWER(?)";
        List<Long> keywords = jdbcTemplate.queryForList(sql, Long.class, "%" + keyword + "%");

        if (keywords.isEmpty()) {
            model.addAttribute("error", "No posts found in this category.");
            return "filter"; // Return with error message if no category found
        }

        Long keywordId = keywords.get(0);
        Long authorId = (Long) request.getSession().getAttribute("authorId");

        if( authorId != null && authorId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		model.addAttribute("personalImage",null);
        	}else {
        		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	model.addAttribute("personalImage",null);
        }
        
        // Fetch posts based on keyword ID
        sql = """
            SELECT 
                p.articleid, 
                       p.primaryAuthor AS author, 
                p.title, 
                p.description, 
                p.likes, 
                p.dislikes, 
                p.viewscount, 
                p.commentscount AS comments, 
                p.updatedat, 
                p.postmedia AS media,
                    p.poststatus AS status,
                u.name AS name, 
                u.username AS username, 
                u.bio AS bio, 
                u.profilepicture AS image,
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
                     p.viewscount, p.commentscount, p.updatedat, 
                     u.name, u.username, u.bio, c.name
            """;
        
        List<String> colors = new ArrayList<>(
    			List.of(
                    "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                    "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                    "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                    "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                    "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                    "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                    "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                    "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                    "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"
    			)
    		);
    	model.addAttribute("colors", colors);

        List<Map<String, Object>> posts = jdbcTemplate.query(sql, (rs, rowNum) -> {
        	Map<String, Object> post = new HashMap<>();
            Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
                
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
            
            post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
            post.put("title", rs.getString("title"));
            post.put("disable",false);
            post.put("description", rs.getString("description"));
            post.put("likes", rs.getInt("likes"));
            post.put("dislikes", rs.getInt("dislikes"));
            post.put("viewscount", rs.getInt("viewscount"));
            post.put("comments", rs.getInt("comments"));
            Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
            post.put("name", rs.getString("name"));
            post.put("username", rs.getString("username"));
            post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
            post.put("category", rs.getString("category"));
            if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
            if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

            // Separate query to get keywords for the current article
            String keywordQuery = """
            		SELECT name FROM Keyword k 
            		JOIN KeywordAssignment ka 
            		ON k.keywordid = ka.keywordid 
            		WHERE ka.articleid = ?
            	""";
        	List<String> keywordsOfPost = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
        	post.put("keywords", keywordsOfPost);
        	
        	if( authorId != null && authorId > 0 ) {

        		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
        		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
        	
            	post.put("isLiked", isReact == 0 ? false : true);
            	
            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
            	post.put("isDisliked", isReact == 0 ? false : true );
        	
        	}else {
        		post.put("isLiked", false );            		
        		post.put("isDisliked", false );
        	}
        	
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

        model.addAttribute("topic", this.capitalize(keyword) );
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
                    "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                    "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                    "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                    "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                    "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                    "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                    "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                    "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                    "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"
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
        Long userId = (Long) request.getSession().getAttribute("authorId");
        if (userId == null) {
            System.out.println("User not logged in. Redirecting to login.");
            model.addAttribute("error", "You need to log in to view your profile.");
            return "redirect:/login";
        }
        
        if( userId != null && userId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, userId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		model.addAttribute("personalImage",null);
        	}else {
        		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	model.addAttribute("personalImage",null);
        }
        
        // Query user data from the database
        String sql = "SELECT name, username, bio, email, profilePicture AS image, updatedat FROM Blogger WHERE authorId = ?";
        Map<String, Object> blogger = jdbcTemplate.queryForMap(sql, userId);
        
        System.out.print(sql + " " + blogger.toString());
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
        Timestamp updatedAt = (Timestamp) blogger.get("updatedat");
        
        if (updatedAt != null) {
            blogger.put("updatedat", sdf.format(updatedAt));
        }

        // Add user data to the model
        model.addAttribute("user", blogger);
        blogger = null;

        sql = "SELECT authorId,name,username,profilePicture AS image,createdat, bio FROM Blogger WHERE authorId = ?";
        List<Map<String, Object>> bloggers = jdbcTemplate.queryForList(sql, userId);
        sdf = new SimpleDateFormat("MMMM yyyy");
        
        for (Map<String, Object> current : bloggers) {
            Timestamp createdat = (Timestamp) current.get("createdat");
            
//            System.out.println("Blogger Is " + blogger );
            Long author = (current.get("authorid") != null) ? (Long) current.get("authorId") : null;

            try {
                List<Map<String, Object>> result = jdbcTemplate.queryForList("""
                        SELECT column_name, data_type 
                        FROM information_schema.columns 
                        WHERE table_name = 'Connection';
                        """);
                System.out.print(result);
                
                sql = """
	            		SELECT COUNT(*) AS followings FROM Connection WHERE followerId = 
	            		""" + author;
	            current.put("followings", jdbcTemplate.queryForObject(sql, Long.class)	 );

	            sql = """
	            		SELECT COUNT(*) AS followers FROM Connection WHERE followingId = 
	            		""" + author;
	            current.put("followers", jdbcTemplate.queryForObject(sql, Long.class) );
	
	            Long likes = 0L;
            	
	            sql = "SELECT articleId FROM Post WHERE primaryAuthor = ?";
	            List<Long> articles = jdbcTemplate.queryForList(sql, Long.class, author);
	            
	            for( Long articleId : articles ) {
	            
	            	sql = """
		            		SELECT COUNT(*) AS likes FROM PostInteraction WHERE articleId = 
		            	""" + articleId + " AND reactionType = 'like' ";
		            likes += jdbcTemplate.queryForObject(sql, Long.class);
	            
	            }
		            
	            current.put("likes", likes);
	            
	            sql = "SELECT articleId FROM Post WHERE primaryAuthor = " + author;
            	List<Long> ids = jdbcTemplate.queryForList(sql, Long.class);
            	
            	Long comments = 0L;
            	
            	for( Long id : ids ) {
            		
            		sql = """
	            		SELECT commentscount FROM Post WHERE articleId = 
	            	""" +  id;
            		
            		comments += jdbcTemplate.queryForObject(sql, Long.class);
            		
            	}
	            current.put("comments", comments );

	            current.put("status", false);
	            
	            sql = """
	            		SELECT COUNT(*) AS posts FROM Post WHERE primaryAuthor =
	            		""" + author;
	            current.put("posts", jdbcTemplate.queryForObject(sql, Long.class) );
	            
	        }catch( Exception e ) {
            	e.printStackTrace();
            	System.out.print("\n\n5" + "\n\n");
            	current.put("posts", 0);
            	current.put("followings", 0);
            	current.put("followingsList", null);
            	current.put("followers", 0);
            	current.put("followersList", null);
            	current.put("likes", 0);
            	current.put("status", false);
            	current.put("comments", 0);
	        }                       
            
            if (createdat != null) {
            	current.put("createdat", sdf.format(createdat));
            }
            
            if( current.get("image") == null || current.get("image").equals("") ) {
            	current.put("image", null);
            }else {
            	current.put("image", bloggerRetrieveDirectory + current.get("image"));
            }
            
        }
        model.addAttribute("bloggers", bloggers);
        
        try {
            // Fetch all IDs the current author is following
            sql = """
                  SELECT followingId FROM Connection WHERE followerId = 
                  """ + userId + " GROUP BY followingId";
            List<Long> followingIds = jdbcTemplate.queryForList(sql, Long.class);

            List<Map<String, Object>> followingDetails = new ArrayList<>();
            
            for( Long id : followingIds ) {
            	 sql = """
                      SELECT authorId, name, username, bio, profilePicture AS image 
                      FROM Blogger 
                      WHERE authorId = """ + id;
            	 List<Map<String, Object>> temp = jdbcTemplate.queryForList(sql);
            	 if( temp.get(0).get("image") == null || temp.get(0).get("image").equals("") ) {
            		 temp.get(0).put("image", null);
                 }else {
                	 temp.get(0).put("image", bloggerRetrieveDirectory + temp.get(0).get("image"));
                 }
            	 followingDetails.add( temp.get(0) );
            }
            model.addAttribute("followingsList", followingDetails);
            
            // Fetch details of these bloggers
            if (followingIds.isEmpty()) {
            	model.addAttribute("followingsList", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("\n\nError fetching followings\n\n");
            model.addAttribute("followingsList", null);
        }

        // Step 2: Fetch all follower IDs
        try {
            // Fetch all IDs of users who follow the current author
            sql = """
                  SELECT followerId FROM Connection WHERE followingId = 
                  """ + userId + " GROUP BY followerId";
            List<Long> followerIds = jdbcTemplate.queryForList(sql, Long.class);

            // Fetch details of these bloggers
            List<Map<String, Object>> followerDetails = new ArrayList<>();
            
            for( Long id : followerIds ) {
            	 sql = """
                      SELECT authorId, name, username, bio, profilePicture AS image 
                      FROM Blogger 
                      WHERE authorId = """ + id;
            	 List<Map<String, Object>> temp = jdbcTemplate.queryForList(sql);
            	 if( temp.get(0).get("image") == null || temp.get(0).get("image").equals("") ) {
            		 temp.get(0).put("image", null);
                 }else {
                	 temp.get(0).put("image", bloggerRetrieveDirectory + temp.get(0).get("image"));
                 }
            	 followerDetails.add( temp.get(0) );

            }
            model.addAttribute("followersList", followerDetails);
            
            // Fetch details of these bloggers
            if (followerIds.isEmpty()) {
                model.addAttribute("followersList", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("\n\nError fetching followings\n\n");
            model.addAttribute("followersList", null);
        }
        
        if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
        String categorySql = "SELECT name FROM Category";
        List<String> categories = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("topics", categories);

        String postSql = """
                SELECT p.articleid, 
                       p.primaryAuthor AS author, 
                       p.title, 
                       p.description, 
                       p.likes, 
                       p.dislikes, 
                       p.viewscount, 
                       p.commentscount AS comments, 
                       p.updatedat, 
                       p.postmedia AS media,
                    p.poststatus AS status,
                       u.name AS name, 
                       u.username AS username, 
                       u.bio AS bio, 
                       u.profilepicture AS image,
                       c.name AS category
                FROM Post p 
                JOIN Blogger u ON p.primaryAuthor = u.authorid 
                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
                JOIN Category c ON pca.categoryid = c.categoryid
                WHERE u.authorid = ? ORDER BY p.createdat DESC
            """;
        
        	Long authorId = (Long) request.getSession().getAttribute("authorId");

            List<Map<String, Object>> posts = jdbcTemplate.query(postSql, (rs, rowNum) -> {
                Map<String, Object> post = new HashMap<>();
                Long articleId = (Long) rs.getLong("articleid");
                
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
                
                post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
                post.put("title", rs.getString("title"));
                post.put("disable",false);
                post.put("description", rs.getString("description"));
                post.put("likes", rs.getInt("likes"));
                post.put("dislikes", rs.getInt("dislikes"));
                post.put("viewscount", rs.getInt("viewscount"));
                post.put("comments", rs.getInt("comments"));
                Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat ssdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = ssdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
                post.put("name", rs.getString("name"));
                post.put("username", rs.getString("username"));
                post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
                post.put("category", rs.getString("category"));
                if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
                if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

                // Separate query to get keywords for the current article
                String keywordQuery = """
                		SELECT name FROM Keyword k 
                		JOIN KeywordAssignment ka 
                		ON k.keywordid = ka.keywordid 
                		WHERE ka.articleid = ?
                	""";
            	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
            	post.put("keywords", keywords);

            	if( authorId != null && authorId > 0 ) {

            		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
            		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
	            	post.put("isLiked", isReact == 0 ? false : true);
	            	
	            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
	            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	            	
	            	post.put("isDisliked", isReact == 0 ? false : true );
            	
            	}else {
            		post.put("isLiked", false );            		
            		post.put("isDisliked", false );
            	}
            	
                return post;
            }, authorId);

        model.addAttribute("posts", posts);

        System.out.print(posts);

    	List<String> colors = new ArrayList<>(
    			List.of(
                    "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                    "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                    "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                    "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                    "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                    "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                    "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                    "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                    "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"	
                )
    		);
    	
    	model.addAttribute("colors", colors);
    	model.addAttribute("followers", null);
    	model.addAttribute("followings", null);
        
        // Add logged-in username if available
        String loggedInUser = (String) request.getSession().getAttribute("loggedInUser");
        model.addAttribute("loggedInUser", loggedInUser != null ? loggedInUser : "Guest");
        
        return "profile"; // Return the profile management view
    }
    
    @PostMapping("/search")
    public String searchKeyword(Model model, String keyword, HttpServletRequest request) {
        if (keyword == null || keyword.trim().isEmpty()) {
            model.addAttribute("error", "Please enter a valid search term.");
            model.addAttribute("posts", null);
            return "filter";
        }
        
        String likePattern = "%" + keyword + "%";        

        Long authorId = (Long) request.getSession().getAttribute("authorId");
        
        if( authorId != null && authorId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		model.addAttribute("personalImage",null);
        	}else {
        		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	model.addAttribute("personalImage",null);
        }
        
        String sql = """
                SELECT p.articleid, 
                       p.primaryAuthor AS author, 
                p.title, 
                p.description, 
                p.likes, 
                p.dislikes, 
                p.viewscount, 
                p.commentscount AS comments, 
                p.updatedat, 
                p.postmedia AS media,
                    p.poststatus AS status,
                u.name AS name, 
                u.username AS username, 
                u.bio AS bio, 
                u.profilepicture AS image,
                c.name AS category
	         FROM Post p 
	         JOIN Blogger u ON p.primaryAuthor = u.authorid 
	         JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
	         JOIN Category c ON pca.categoryid = c.categoryid 
	         WHERE p.title ILIKE ? 
                OR p.description ILIKE ? 
                OR c.name ILIKE ?
	     """;
	
	     List<Map<String, Object>> searchPosts = jdbcTemplate.query(sql, (rs, rowNum) -> {
	         Map<String, Object> post = new HashMap<>();
	         Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
	         
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
	         post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
	         post.put("title", rs.getString("title"));
	         post.put("disable",false);
	         post.put("description", rs.getString("description"));
	         post.put("likes", rs.getInt("likes"));
	         post.put("dislikes", rs.getInt("dislikes"));
	         post.put("viewscount", rs.getInt("viewscount"));
	         post.put("comments", rs.getInt("comments"));
	         Timestamp timestamp = rs.getTimestamp("updatedat");
	         if (timestamp != null) {
	             SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
	             String formattedDate = sdf.format(timestamp);
	             post.put("updatedat", formattedDate);
	         } else {
	             post.put("updatedat", null);
	         }
	         post.put("name", rs.getString("name"));
	         post.put("username", rs.getString("username"));
	         post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
	         post.put("category", rs.getString("category"));
             if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
	         if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}
	
	         // Separate query to get keywords for the current article
	         String keywordQuery = """
	         		SELECT name FROM Keyword k 
	         		JOIN KeywordAssignment ka 
	         		ON k.keywordid = ka.keywordid 
	         		WHERE ka.articleid = ?
	         	""";
	     	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
	     	post.put("keywords", keywords);
	
        	if( authorId != null && authorId > 0 ) {

        		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
        		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
        	
            	post.put("isLiked", isReact == 0 ? false : true);
            	
            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
            	post.put("isDisliked", isReact == 0 ? false : true );
        	
        	}else {
        		post.put("isLiked", false );            		
        		post.put("isDisliked", false );
        	}
	     	
	         return post;
	     
	 }, likePattern, likePattern, likePattern);
	     
	 List<Map<String, Object>> categoriesPosts = new ArrayList<>();
	 List<Map<String, Object>> keywordsPosts = new ArrayList<>();
	 
	 sql = "SELECT categoryId FROM Category WHERE LOWER(name) LIKE LOWER(?)";
     List<Long> categories = jdbcTemplate.queryForList(sql, Long.class, "%" + keyword + "%");

     if (categories.isEmpty()) {
         model.addAttribute("error", "No posts found in this category.");

     }else {
	     
	     sql = """
	                SELECT p.articleid, 
                       p.primaryAuthor AS author, 
	                p.title, 
	                p.description, 
	                p.likes, 
	                p.dislikes, 
	                p.viewscount, 
	                p.commentscount AS comments, 
	                p.updatedat, 
	                p.postmedia AS media,
                    p.poststatus AS status,
	                u.name AS name, 
	                u.username AS username, 
	                u.bio AS bio, 
	     		    u.profilepicture AS image,
	                c.name AS category
	         FROM Post p 
	         JOIN Blogger u ON p.primaryAuthor = u.authorid 
	         JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
	         JOIN Category c ON pca.categoryid = c.categoryid
	         WHERE c.categoryid = ? 
	     """;

	     categoriesPosts = jdbcTemplate.query(sql, (rs, rowNum) -> {
	         Map<String, Object> post = new HashMap<>();
	         Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
	         
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
	         post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
	         post.put("title", rs.getString("title"));
	         post.put("disable",false);
	         post.put("description", rs.getString("description"));
	         post.put("likes", rs.getInt("likes"));
	         post.put("dislikes", rs.getInt("dislikes"));
	         post.put("viewscount", rs.getInt("viewscount"));
	         post.put("comments", rs.getInt("comments"));
	         Timestamp timestamp = rs.getTimestamp("updatedat");
	                if (timestamp != null) {
	                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
	                    String formattedDate = sdf.format(timestamp);
	                    post.put("updatedat", formattedDate);
	                } else {
	                    post.put("updatedat", null);
	                }
	         post.put("name", rs.getString("name"));
	         post.put("username", rs.getString("username"));
	         post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
	         post.put("category", rs.getString("category"));
             if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
	         if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

	         // Separate query to get keywords for the current article
	         String keywordQuery = """
	         		SELECT name FROM Keyword k 
	         		JOIN KeywordAssignment ka 
	         		ON k.keywordid = ka.keywordid 
	         		WHERE ka.articleid = ?
	         	""";
	     	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
	     	post.put("keywords", keywords);
	     	
        	if( authorId != null && authorId > 0 ) {

        		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
        		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
        	
            	post.put("isLiked", isReact == 0 ? false : true);
            	
            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
            	post.put("isDisliked", isReact == 0 ? false : true );
        	
        	}else {
        		post.put("isLiked", false );            		
        		post.put("isDisliked", false );
        	}
	     	
	     	return post;
	     	
	     }, categories.get(0));
	     
     }
	     
	     sql = "SELECT keywordId FROM Keyword WHERE LOWER(name) LIKE LOWER(?)";
	        List<Long> keywords = jdbcTemplate.queryForList(sql, Long.class, "%" + keyword + "%");

	        if (keywords.isEmpty()) {
	            model.addAttribute("error", "No posts found in this category."); // Return with error message if no category found
	        }else {

	        Long keywordId = keywords.get(0);

	        // Fetch posts based on keyword ID
	        sql = """
	            SELECT 
	                p.articleid, 
                       p.primaryAuthor AS author, 
	                p.title, 
	                p.description, 
	                p.likes, 
	                p.dislikes, 
	                p.viewscount, 
	                p.commentscount AS comments, 
	                p.updatedat, 
	                p.postmedia AS media,
                    p.poststatus AS status,
	                u.name AS name, 
	                u.username AS username, 
	                u.bio AS bio, 
	        		u.profilepicture AS image,
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
	                     p.viewscount, p.commentscount, p.updatedat,
	                     u.name, u.username, u.bio, c.name, p.primaryAuthor
	            """;

	        keywordsPosts = jdbcTemplate.query(sql, (rs, rowNum) -> {
	        	Map<String, Object> post = new HashMap<>();
	            Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
	            
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
	            post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
	            post.put("title", rs.getString("title"));
	            post.put("disable",false);
	            post.put("description", rs.getString("description"));
	            post.put("likes", rs.getInt("likes"));
	            post.put("dislikes", rs.getInt("dislikes"));
	            post.put("viewscount", rs.getInt("viewscount"));
	            post.put("comments", rs.getInt("comments"));
	            Timestamp timestamp = rs.getTimestamp("updatedat");
	                if (timestamp != null) {
	                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
	                    String formattedDate = sdf.format(timestamp);
	                    post.put("updatedat", formattedDate);
	                } else {
	                    post.put("updatedat", null);
	                }
	            post.put("name", rs.getString("name"));
	            post.put("username", rs.getString("username"));
	            post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
	            post.put("category", rs.getString("category"));
                if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
	            if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

	            // Separate query to get keywords for the current article
	            String keywordQuery = """
	            		SELECT name FROM Keyword k 
	            		JOIN KeywordAssignment ka 
	            		ON k.keywordid = ka.keywordid 
	            		WHERE ka.articleid = ?
	            	""";
	        	List<String> keywordsOfPost = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
	        	post.put("keywords", keywordsOfPost);
	        	
	        	if( authorId != null && authorId > 0 ) {

	        		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
	        		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	        	
	            	post.put("isLiked", isReact == 0 ? false : true);
	            	
	            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
	            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	            	
	            	post.put("isDisliked", isReact == 0 ? false : true );
	        	
	        	}else {
	        		post.put("isLiked", false );            		
	        		post.put("isDisliked", false );
	        	}
	        	
	        	return post;
	        } ,keywordId);
	        
	    }
	     
	
	    List<String> colors = new ArrayList<>(
	    		List.of(
                    "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                    "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                    "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                    "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                    "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                    "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                    "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                    "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                    "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"                )
            );
	    
	    model.addAttribute("colors", colors);

	    List<Map<String, Object> > posts = new ArrayList<>();
	    
	    for( Map<String,Object> post : searchPosts ) {
	    	if( ! posts.contains(post) ) {
	    		posts.add(post);
	    	}
	    }
	    
	    for( Map<String,Object> post : categoriesPosts ) {
	    	if( ! posts.contains(post) ) {
	    		posts.add(post);
	    	}
	    }
	    
	    for( Map<String,Object> post : keywordsPosts ) {
	    	if( ! posts.contains(post) ) {
	    		posts.add(post);
	    	}
	    }
	    
	    model.addAttribute("posts", posts);
		
		System.out.print(posts);
		 

        if (posts.isEmpty()) {
            model.addAttribute("error", "No posts found for the search term.");
        } else {
            model.addAttribute("posts", posts);
        }

        // Pass keyword and logged-in user info to the view
        model.addAttribute("topic", this.capitalize(keyword) );
        model.addAttribute("loggedInUser", (userExist != null && !userExist.isEmpty()) ? userExist : null);

        return "filter";
    }
    
    @PostMapping("/doPost")
    public String makePost(Model model, HttpServletRequest request, String id) {
    	
    	 Long primaryAuthorId = (Long) request.getSession().getAttribute("authorId");

	    // Convert the received ID to Long
	    Long articleId = Long.valueOf(id);

	    // Define the SQL update query for PostgreSQL
	    String sql = "UPDATE Post SET postStatus = 'published' WHERE articleId = ? AND primaryAuthor = ?";

	    // Execute the query using JdbcTemplate or a similar mechanism
	    try {
	        jdbcTemplate.update(sql, articleId, primaryAuthorId);
	    } catch (Exception e) {
	        System.out.println("error" + "Unable to update post status: " + e.getMessage());
	        return "redirect:/error"; // Redirect to an error page or show an error message
	    }

	    return "redirect:/";
    	
    }

    /*@PostMapping("/doPost")
    public String makePost(Model model, String title, String category, String description, String keyword, String image, HttpServletRequest request) {
        try {
        	
        	System.out.print( title + " " + category+ " " + description + " " + keyword + " " + image);
            // Step 1: Clean the description
            description = description.replaceAll("<[^>]*>", "").trim();
            Long primaryAuthorId = (Long) request.getSession().getAttribute("authorId");;  // Assuming logged-in user ID

            Long newArticleId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(articleid), 0) + 1 FROM Post", Long.class);
            
            String media = "";
            
            if( image == null ) {
            	media = "";
            }else {
            	String[] parts = image.split("\\.");
            	String extension = parts[parts.length - 1];

            	// Construct the media file name
            	media = "post-" + newArticleId + "." + extension;
            	System.out.println("Image" + image);
            	System.out.println("Media: " + media);
            	
            	Path oldFile = Paths.get(postStoreDirectory + image);
                
                // Specify the new file name
                Path newFile = Paths.get(postStoreDirectory + media);
 
                try {
                    // Check if the old file exists
                    if (Files.exists(oldFile)) {
                        // Copy the file
                        Files.copy(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("File copied successfully.");

                        // Delete the old file
                        Files.delete(oldFile);
                        System.out.println("File deleted successfully.");
                    } else {
                        System.out.println("Source file does not exist: " + oldFile.toString());
                    }
                } catch (Exception e) {
                    System.out.println("Error occurred: " + e.getMessage());
                }
            	
            }
            
            // Step 2: Insert Post
            String insertPostSql = """
                INSERT INTO Post (articleid, title, description, likes, dislikes, commentscount, primaryauthor, viewscount, postmedia, publishedat, createdat, updatedat) 
                VALUES (?, ?, ?, 0, 0, 0, ?, 0, ? , CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;
            
            int postRowsAffected = jdbcTemplate.update(insertPostSql, newArticleId, title, description, primaryAuthorId, media);
            System.out.println("Post Inserted. Rows affected: " + postRowsAffected);

            // Step 3: Insert into PostCategoryAssignment
            String getCategoryIdSql = "SELECT categoryid FROM Category WHERE LOWER(name) = LOWER(?)";
            Long categoryId = null;
            try {
                categoryId = jdbcTemplate.queryForObject(getCategoryIdSql, Long.class, category);
            } catch (EmptyResultDataAccessException e) {
                System.out.println("Category not found: " + category);
            }
            
            if (categoryId != null) {
            	
            	Long postCategoryAssignmnetId = jdbcTemplate.queryForObject( """
            			SELECT COALESCE(MAX(postCategoryAssignmentId), 0) + 1 FROM PostCategoryAssignment
            			""", Long.class);
            	
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
                    Long keywordId = null;
                    try {
                        keywordId = jdbcTemplate.queryForObject(getKeywordIdSql, Long.class, "%" + currentKeyword.trim() + "%");
                    } catch (EmptyResultDataAccessException e) {
                        System.out.println("Keyword not found: " + currentKeyword);
                    }
                    
                    if (keywordId != null) {
                    	
                    	Long keywordAssignmentId = jdbcTemplate.queryForObject( """
                    			SELECT COALESCE(MAX(keywordAssignmentId), 0) + 1 FROM KeywordAssignment
                    		""", Long.class);
                    	
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
            return "redirect:/create_post?error=true"; // Return to the post creation page in case of an error
        }

        return "redirect:/"; // Redirect to post listing page
    }	*/
    
    /* Create a new post
    @PostMapping("/doPost")
    public String makePost(Model model, String title, String categoryName, String description, String keyword) {
        try {
            // Clean up the description by removing HTML tags and trimming
            description = description.replaceAll("<[^>]*>", "").trim();
            int primaryAuthorId = 1; // Change as per your logic

            // Generate a new article ID
            String getMaxIdSql = "SELECT COALESCE(MAX(articleid), 0) + 1 FROM Post";
            Long newArticleId = jdbcTemplate.queryForObject(getMaxIdSql, Long.class);

            // Insert the new post
            String insertPostSql = """
                INSERT INTO Post (articleid, title, description, likes, dislikes, commentscount, primaryauthor, viewscount, postmedia, publishedat, createdat, updatedat) 
                VALUES (?, ?, ?, 0, 0, 0, ?, 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;
            int postRowsAffected = jdbcTemplate.update(insertPostSql, newArticleId, title, description, primaryAuthorId);
            System.out.println("Post Inserted. Rows affected: " + postRowsAffected);

            // Insert into PostCategoryAssignment
            String getCategoryIdSql = "SELECT categoryid FROM Category WHERE  LOWER(name) LIKE LOWER(?)";
            Long categoryId = jdbcTemplate.queryForObject(getCategoryIdSql, Long.class, categoryName);

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
                    Long keywordId = jdbcTemplate.queryForObject(getKeywordIdSql, Long.class, currentKeyword);

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

    
    @GetMapping("/load-more-posts")
    @ResponseBody
    public Map<String, Object> loadMorePost(@RequestParam("page") int page, HttpServletRequest request) {
        int pageSize = 5; // Number of posts per page
        int offset = (page - 1) * pageSize;

        Long authorId = (Long) request.getSession().getAttribute("authorId");
        
        // SQL query to fetch posts with pagination
        String postSql = """
                SELECT p.articleid, 
                       p.primaryAuthor AS author, 
                       p.title, 
                       p.description, 
                       p.likes, 
                       p.dislikes, 
                       p.viewscount, 
                       p.commentscount AS comments, 
                       p.updatedat, 
                       p.postmedia AS media,
                    p.poststatus AS status,
                       u.name AS name, 
                       u.username AS username, 
                       u.bio AS bio, 
                       u.profilepicture AS image,
                       c.name AS category
                FROM Post p 
                JOIN Blogger u ON p.primaryAuthor = u.authorid 
                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
                JOIN Category c ON pca.categoryid = c.categoryid
                ORDER BY p.createdat DESC
                LIMIT ? OFFSET ?
            """;

        List<Map<String, Object>> posts = jdbcTemplate.query(postSql, (rs, rowNum) -> {
            Map<String, Object> post = new HashMap<>();
            Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");

                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
            post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
            post.put("title", rs.getString("title"));
            post.put("disable",false);
            post.put("description", rs.getString("description"));
            post.put("likes", rs.getInt("likes"));
            post.put("dislikes", rs.getInt("dislikes"));
            post.put("viewscount", rs.getInt("viewscount"));
            post.put("comments", rs.getInt("comments"));
            Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
            post.put("name", rs.getString("name"));
            post.put("username", rs.getString("username"));
            post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
            post.put("category", rs.getString("category"));
            if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
            if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

            // Fetch keywords for the current article
            String keywordQuery = """
                SELECT name 
                FROM Keyword k 
                JOIN KeywordAssignment ka ON k.keywordid = ka.keywordid 
                WHERE ka.articleid = ?
            """;
            List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, articleId);
            post.put("keywords", keywords);

        	if( authorId != null && authorId > 0 ) {

        		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
        		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
        	
            	post.put("isLiked", isReact == 0 ? false : true);
            	
            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
            	post.put("isDisliked", isReact == 0 ? false : true );
        	
        	}else {
        		post.put("isLiked", false );            		
        		post.put("isDisliked", false );
        	}
            
            return post;
        }, pageSize, offset);

        // Check if there are more posts to load
        boolean hasMore = posts.size() == pageSize; 

        String newsApiUrl = "https://newsapi.org/v2/everything";
        String url = UriComponentsBuilder.fromHttpUrl(newsApiUrl)
                .queryParam("q", searchKeyword)
                .queryParam("page", page - 1)
                .queryParam("pageSize", pageSize)
                .queryParam("apiKey", dotenv.get("NEWS_API"))
                .toUriString();
        RestTemplate restTemplate = new RestTemplate();
        HashMap<String, Object> results;
        
        try {
            // Fetching the response as a HashMap
            results = restTemplate.getForObject(url, HashMap.class);
            
            
        } catch (HttpClientErrorException e) {
            // Handling errors if the API call fails (e.g., invalid API key, quota exceeded)
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "API call failed: " + e.getMessage());
            return errorResponse;
        }
        int totalResults = (int) results.get("totalResults");

        List<HashMap<String, Object>> result = (List<HashMap<String, Object>>) results.get("articles");
        // Prepare response

        String keywordSql = "SELECT name FROM Keyword";
        List<String> keywords = jdbcTemplate.queryForList(keywordSql, String.class);
       
        
        for( HashMap<String, Object> post : result ) {

        	List<String> tempKeywords = new LinkedList<>();
        	HashMap<String, Object> tempPost = new HashMap<>();
            Long articleId = 0l;

            tempPost.put("articleid", articleId);
            tempPost.put("author", null);
            tempPost.put("title", post.get("title") );
            
            String content = post.get("content") != null ? post.get("content").toString() : "";
            String[] splitContent = content.split("…"); // Split at '...' only once
            String description = splitContent[0]; // Take the first part

            // Append the 'Read more...' button
            description += " <button class=\"toggle-button\" style=\"width:auto;\" onclick=\" location.href='" 
            + post.get("url") 
            + "'; \"> Read more... </button> ";
            
            tempPost.put("description", description );
            tempPost.put("likes", 0);
            tempPost.put("dislikes", 0);
            tempPost.put("viewscount", 0);
            tempPost.put("comments", 0);
            try {
                // Parse the ISO 8601 date-time string to a java.util.Date
                Instant instant = Instant.parse( post.get("publishedAt").toString() );
                Timestamp timestamp = Timestamp.from(instant);

                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    tempPost.put("updatedat", formattedDate);
                } else {
                    tempPost.put("updatedat", null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                tempPost.put("updatedat", null);
            }
            
            tempPost.put("name", post.get("author"));
            tempPost.put("postComments", null);
            tempPost.put("status", "published");
            tempPost.put("username", "tech2xplore");
            tempPost.put("bio", "Tech2Xplore Trending News");
            tempPost.put("category", "News");
            tempPost.put("media", post.get("urlToImage"));
            tempPost.put("image", null);
            tempPost.put("isLiked", false);
            tempPost.put("isDisliked", false);
            tempPost.put("disable", true);

            // Fetch keywords for the current article
            for( String keyword: keywords ) {
            	if( 
            			tempPost.get("description").toString().toLowerCase().contains( keyword.toLowerCase() ) 	||
            			tempPost.get("title").toString().toLowerCase().contains( keyword.toLowerCase() )		
            		) {
            		tempKeywords.add( keyword );
            	}
            }
            
            tempPost.put("keywords", tempKeywords.size() == 0 ? null : tempKeywords );

            
            if( tempPost.get("name") == null || tempPost.get("name") == "" ) {
            	tempPost.put("name", "Tech2Xplore Trending News");
            }else {
            	tempPost.put("name", tempPost.get("name") + " via Tech2Xplore and News API");
            }
            
            if( 
            		tempPost.get("title").equals("[Removed]") 	||
            		tempPost.get("description").equals("[Removed]") 
            		
            ) {	}else {
            	posts.add( tempPost );
            }
        	
        }
        
        hasMore = hasMore || ( totalResults > page * pageSize );
        
        System.out.print("\n\n\n\n\nPosts : " + posts + "\n\n\n\n");
        Map<String, Object> response = new HashMap<>();
     
        if( authorId != null && authorId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		response.put("personalImage",null);
        	}else {
        		response.put("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	response.put("personalImage",null);
        }
        
        response.put("posts", posts);
        response.put("hasMore", hasMore);

        return response;
    }
    
    @GetMapping("/load-more-posts2")
    public String loadMorePost2(@RequestParam("page") int page, HttpServletRequest request, Model model) {
        int pageSize = 5; // Number of posts per page
        int offset = (page - 1) * pageSize;

        Long authorId = (Long) request.getSession().getAttribute("authorId");
        
        // SQL query to fetch posts with pagination
        String postSql = """
                SELECT p.articleid, 
                       p.primaryAuthor AS author, 
                       p.title, 
                       p.description, 
                       p.likes, 
                       p.dislikes, 
                       p.viewscount, 
                       p.commentscount AS comments, 
                       p.updatedat, 
                       p.postmedia AS media,
                    p.poststatus AS status,
                       u.name AS name, 
                       u.username AS username, 
                       u.bio AS bio, 
                       u.profilepicture AS image,
                       c.name AS category
                FROM Post p 
                JOIN Blogger u ON p.primaryAuthor = u.authorid 
                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
                JOIN Category c ON pca.categoryid = c.categoryid
                ORDER BY p.createdat DESC
                LIMIT ? OFFSET ?
            """;

        List<Map<String, Object>> posts = jdbcTemplate.query(postSql, (rs, rowNum) -> {
            Map<String, Object> post = new HashMap<>();
            Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");

                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
            post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
            post.put("title", rs.getString("title"));
            post.put("disable",false);
            post.put("description", rs.getString("description"));
            post.put("likes", rs.getInt("likes"));
            post.put("dislikes", rs.getInt("dislikes"));
            post.put("viewscount", rs.getInt("viewscount"));
            post.put("comments", rs.getInt("comments"));
            Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
            post.put("name", rs.getString("name"));
            post.put("username", rs.getString("username"));
            post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
            post.put("category", rs.getString("category"));
            if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
            if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

            // Fetch keywords for the current article
            String keywordQuery = """
                SELECT name 
                FROM Keyword k 
                JOIN KeywordAssignment ka ON k.keywordid = ka.keywordid 
                WHERE ka.articleid = ?
            """;
            List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, articleId);
            post.put("keywords", keywords);

        	if( authorId != null && authorId > 0 ) {

        		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
        		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
        	
            	post.put("isLiked", isReact == 0 ? false : true);
            	
            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
            	post.put("isDisliked", isReact == 0 ? false : true );
        	
        	}else {
        		post.put("isLiked", false );            		
        		post.put("isDisliked", false );
        	}
            
            return post;
        }, pageSize, offset);

        // Check if there are more posts to load
        boolean hasMore = posts.size() == pageSize; 

        String newsApiUrl = "https://newsapi.org/v2/everything";
        String url = UriComponentsBuilder.fromHttpUrl(newsApiUrl)
                .queryParam("q", searchKeyword)
                .queryParam("page", page - 1)
                .queryParam("pageSize", pageSize)
                .queryParam("apiKey", dotenv.get("NEWS_API"))
                .toUriString();
        RestTemplate restTemplate = new RestTemplate();
        HashMap<String, Object> results;
        
        try {
            // Fetching the response as a HashMap
            results = restTemplate.getForObject(url, HashMap.class);
            
            
        } catch (HttpClientErrorException e) {
            // Handling errors if the API call fails (e.g., invalid API key, quota exceeded)
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "API call failed: " + e.getMessage());
            return errorResponse.toString();
        }
        
        int totalResults = (int) results.get("totalResults");

        List<HashMap<String, Object>> result = (List<HashMap<String, Object>>) results.get("articles");
        // Prepare response

        String keywordSql = "SELECT name FROM Keyword";
        List<String> keywords = jdbcTemplate.queryForList(keywordSql, String.class);
       
        
        for( HashMap<String, Object> post : result ) {

        	List<String> tempKeywords = new LinkedList<>();
        	HashMap<String, Object> tempPost = new HashMap<>();
            Long articleId = 0l;

            tempPost.put("articleid", articleId);
            tempPost.put("author", null);
            tempPost.put("title", post.get("title") );
            
            String content = post.get("content") != null ? post.get("content").toString() : "";
            String[] splitContent = content.split("…"); // Split at '...' only once
            String description = splitContent[0]; // Take the first part

            // Append the 'Read more...' button
            description += " <button class=\"toggle-button\" style=\"width:auto;\" onclick=\" location.href='" 
            + post.get("url") 
            + "'; \"> Read more... </button> ";
            
            tempPost.put("description", description );
            tempPost.put("likes", 0);
            tempPost.put("dislikes", 0);
            tempPost.put("viewscount", 0);
            tempPost.put("comments", 0);
            try {
                // Parse the ISO 8601 date-time string to a java.util.Date
                Instant instant = Instant.parse( post.get("publishedAt").toString() );
                Timestamp timestamp = Timestamp.from(instant);

                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    tempPost.put("updatedat", formattedDate);
                } else {
                    tempPost.put("updatedat", null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                tempPost.put("updatedat", null);
            }
            
            tempPost.put("name", post.get("author"));
            tempPost.put("postComments", null);
            tempPost.put("status", "published");
            tempPost.put("username", "tech2xplore");
            tempPost.put("bio", "Tech2Xplore Trending News");
            tempPost.put("category", "News");
            tempPost.put("media", post.get("urlToImage"));
            tempPost.put("image", null);
            tempPost.put("isLiked", false);
            tempPost.put("isDisliked", false);
            tempPost.put("disable", true);

            // Fetch keywords for the current article
            for( String keyword: keywords ) {
            	if( 
            			tempPost.get("description").toString().toLowerCase().contains( keyword.toLowerCase() ) 	||
            			tempPost.get("title").toString().toLowerCase().contains( keyword.toLowerCase() )		
            		) {
            		tempKeywords.add( keyword );
            	}
            }
            
            tempPost.put("keywords", tempKeywords.size() == 0 ? null : tempKeywords );

            
            if( tempPost.get("name") == null || tempPost.get("name") == "" ) {
            	tempPost.put("name", "Tech2Xplore Trending News");
            }else {
            	tempPost.put("name", tempPost.get("name") + " via Tech2Xplore and News API");
            }
            
            if( 
            		tempPost.get("title").equals("[Removed]") 	||
            		tempPost.get("description").equals("[Removed]") 
            		
            ) {	}else {
            	posts.add( tempPost );
            }
        	
        }
        
        hasMore = hasMore || ( totalResults > page * pageSize );
        
        System.out.print("\n\n\n\n\nPosts : " + posts + "\n\n\n\n");
        Map<String, Object> response = new HashMap<>();
     
        if( authorId != null && authorId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		response.put("personalImage",null);
        	}else {
        		response.put("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	response.put("personalImage",null);
        }
        
        model.addAttribute("posts", posts);
        model.addAttribute("hasMore", hasMore);

        return "post :: div"; 
    }
    
    
    @GetMapping("/trendings")
    public String trendingPosts(Model model, HttpServletRequest request) {
    	
        if (this.userExist != null && !this.userExist.isEmpty()) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	
    	int page = 1;
        int pageSize = 15;
        
        Long authorId = (Long) request.getSession().getAttribute("authorId");
        if( authorId != null && authorId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		model.addAttribute("personalImage",null);
        	}else {
        		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	model.addAttribute("personalImage",null);
        }

        List<Map<String, Object>> posts = new LinkedList<>();
        
    	if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	

    	String categorySql = "SELECT name FROM Category";
        List<String> categories = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("topics", categories);
    	
        String keywordSql = "SELECT name FROM Keyword";
        List<String> keywords = jdbcTemplate.queryForList(keywordSql, String.class);
       
        
        String newsApiUrl = "https://newsapi.org/v2/everything";
        String url = UriComponentsBuilder.fromHttpUrl(newsApiUrl)
                .queryParam("q", searchKeyword)
                .queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .queryParam("apiKey", dotenv.get("NEWS_API"))
                .toUriString();
        RestTemplate restTemplate = new RestTemplate();
        HashMap<String, Object> results;
        
        try {
            // Fetching the response as a HashMap
            results = restTemplate.getForObject(url, HashMap.class);
            
            
        } catch (HttpClientErrorException e) {
            // Handling errors if the API call fails (e.g., invalid API key, quota exceeded)
            Map<String, Object> errorResponse = new HashMap<>();
            e.printStackTrace();
            errorResponse.put("error", "API call failed: " + e.getMessage());
            return "redirect:/";
        }
        int totalResults = (int) results.get("totalResults");

        List<HashMap<String, Object>> result = (List<HashMap<String, Object>>) results.get("articles");
        // Prepare response
        for( HashMap<String, Object> post : result ) {
        
        	List<String> tempKeywords = new LinkedList<>();
        	HashMap<String, Object> tempPost = new HashMap<>();
            Long articleId = 0l;

            tempPost.put("articleid", articleId);
            tempPost.put("author", null);
            tempPost.put("title", post.get("title") );
            
            String content = post.get("content") != null ? post.get("content").toString() : "";
            String[] splitContent = content.split("…"); // Split at '...' only once
            String description = splitContent[0]; // Take the first part

            // Append the 'Read more...' button
            description += " <button class=\"toggle-button\" style=\"width:auto;\" onclick=\" location.href='" 
            + post.get("url") 
            + "'; \"> Read more... </button> ";
            
            tempPost.put("postComments", null);
            tempPost.put("description", description );
            tempPost.put("likes", 0);
            tempPost.put("dislikes", 0);
            tempPost.put("viewscount", 0);
            tempPost.put("comments", 0);
            try {
                // Parse the ISO 8601 date-time string to a java.util.Date
                Instant instant = Instant.parse( post.get("publishedAt").toString() );
                Timestamp timestamp = Timestamp.from(instant);

                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    tempPost.put("updatedat", formattedDate);
                } else {
                    tempPost.put("updatedat", null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                tempPost.put("updatedat", null);
            }
            tempPost.put("name", post.get("author"));
            tempPost.put("status", "published");
            tempPost.put("username", "tech2xplore");
            tempPost.put("bio", "Tech2Xplore Trending News");
            tempPost.put("category", "News");
            tempPost.put("media", post.get("urlToImage"));
            tempPost.put("image", null);
            tempPost.put("status", "published");
            tempPost.put("isLiked", false);
            tempPost.put("isDisliked", false);
            tempPost.put("disable", true);

            // Fetch keywords for the current article
            for( String keyword: keywords ) {
            	if( 
            			tempPost.get("description").toString().toLowerCase().contains( keyword.toLowerCase() ) 	||
            			tempPost.get("title").toString().toLowerCase().contains( keyword.toLowerCase() )		
            		) {
            		tempKeywords.add( keyword );
            	}
            }
            
            tempPost.put("keywords", tempKeywords.size() == 0 ? null : tempKeywords );

            if( tempPost.get("name") == null || tempPost.get("name") == "" ) {
            	tempPost.put("name", "Tech2Xplore Trending News");
            }else {
            	tempPost.put("name", tempPost.get("name") + " via Tech2Xplore and News API");
            }
            
            if( 
            		tempPost.get("title").equals("[Removed]") 	||
            		tempPost.get("description").equals("[Removed]") 
            		
            ) {	}else {
            	posts.add( tempPost );
            }
        	
        }
        
        model.addAttribute("posts", posts);

//      System.out.print(posts);

  	List<String> colors = new ArrayList<>(
  			List.of(
                "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"	
            )
  		);
  	
  	model.addAttribute("colors", colors);
        
    	return "trending";
    }
    
    @GetMapping("/trending-more-posts")
    @ResponseBody
    public Map<String, Object> trendingMorePost(@RequestParam("page") int page, HttpServletRequest request) {
        int pageSize = 15; // Number of posts per page
        
        List<Map<String, Object>> posts = new LinkedList<>();

        // Check if there are more posts to load
        boolean hasMore = false; 

        String keywordSql = "SELECT name FROM Keyword";
        List<String> keywords = jdbcTemplate.queryForList(keywordSql, String.class);
       
        String newsApiUrl = "https://newsapi.org/v2/everything";
        String url = UriComponentsBuilder.fromHttpUrl(newsApiUrl)
                .queryParam("q", searchKeyword)
                .queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .queryParam("apiKey", dotenv.get("NEWS_API"))
                .toUriString();
        RestTemplate restTemplate = new RestTemplate();
        HashMap<String, Object> results;
        
        try {
            // Fetching the response as a HashMap
            results = restTemplate.getForObject(url, HashMap.class);
            
            
        } catch (HttpClientErrorException e) {
            // Handling errors if the API call fails (e.g., invalid API key, quota exceeded)
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "API call failed: " + e.getMessage());
            return errorResponse;
        }
        int totalResults = (int) results.get("totalResults");

        List<HashMap<String, Object>> result = (List<HashMap<String, Object>>) results.get("articles");
        // Prepare response
        for( HashMap<String, Object> post : result ) {

        	List<String> tempKeywords = new LinkedList<>();
        	HashMap<String, Object> tempPost = new HashMap<>();
            Long articleId = 0l;

            tempPost.put("articleid", articleId);
            tempPost.put("author", null);
            tempPost.put("title", post.get("title") );
            
            String content = post.get("content") != null ? post.get("content").toString() : "";
            String[] splitContent = content.split("…"); // Split at '...' only once
            String description = splitContent[0]; // Take the first part

            // Append the 'Read more...' button
            description += " <button class=\"toggle-button\" style=\"width:auto;\" onclick=\" location.href='" 
            + post.get("url") 
            + "'; \"> Read more... </button> ";
            
            tempPost.put("postComments", null );
            tempPost.put("description", description );
            tempPost.put("likes", 0);
            tempPost.put("dislikes", 0);
            tempPost.put("viewscount", 0);
            tempPost.put("comments", 0);
            try {
                // Parse the ISO 8601 date-time string to a java.util.Date
                Instant instant = Instant.parse( post.get("publishedAt").toString() );
                Timestamp timestamp = Timestamp.from(instant);

                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    tempPost.put("updatedat", formattedDate);
                } else {
                    tempPost.put("updatedat", null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                tempPost.put("updatedat", null);
            }
            tempPost.put("name", post.get("author"));
            tempPost.put("status", "published");
            tempPost.put("username", "tech2xplore");
            tempPost.put("bio", "Tech2Xplore Trending News");
            tempPost.put("category", "News");
            tempPost.put("media", post.get("urlToImage"));
            tempPost.put("image", null);
            tempPost.put("status", "published");
            tempPost.put("isLiked", false);
            tempPost.put("isDisliked", false);
            tempPost.put("disable", true);

            // Fetch keywords for the current article
            for( String keyword: keywords ) {
            	if( 
            			tempPost.get("description").toString().toLowerCase().contains( keyword.toLowerCase() ) 	||
            			tempPost.get("title").toString().toLowerCase().contains( keyword.toLowerCase() )		
            		) {
            		tempKeywords.add( keyword );
            	}
            }
            tempPost.put("keywords", tempKeywords.size() == 0 ? null : tempKeywords );

            if( tempPost.get("name") == null || tempPost.get("name") == "" ) {
            	tempPost.put("name", "Tech2Xplore Trending News");
            }else {
            	tempPost.put("name", tempPost.get("name") + " via Tech2Xplore and News API");
            }
            
            if( 
            		tempPost.get("title").equals("[Removed]") 	||
            		tempPost.get("description").equals("[Removed]") 
            		
            ) {	}else {
            	posts.add( tempPost );
            }
        	
        }
        
        hasMore = hasMore || ( totalResults > page * pageSize );
        
        System.out.print("\n\n\n\n\nPosts : " + posts + "\n\n\n\n");
        Map<String, Object> response = new HashMap<>();
        Long authorId = (Long) request.getSession().getAttribute("authorId");
        
        if( authorId != null && authorId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		response.put("personalImage",null);
        	}else {
        		response.put("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	response.put("personalImage",null);
        }
        
        response.put("posts", posts);
        response.put("hasMore", hasMore);

        return response;
    }


    @GetMapping("/trending-more-posts2")
    public String trendingMorePost2(@RequestParam("page") int page, HttpServletRequest request, Model model) {
        int pageSize = 15; // Number of posts per page
        
        List<Map<String, Object>> posts = new LinkedList<>();

        // Check if there are more posts to load
        boolean hasMore = false; 

        String keywordSql = "SELECT name FROM Keyword";
        List<String> keywords = jdbcTemplate.queryForList(keywordSql, String.class);
       
        String newsApiUrl = "https://newsapi.org/v2/everything";
        String url = UriComponentsBuilder.fromHttpUrl(newsApiUrl)
                .queryParam("q", searchKeyword)
                .queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .queryParam("apiKey", dotenv.get("NEWS_API"))
                .toUriString();
        RestTemplate restTemplate = new RestTemplate();
        HashMap<String, Object> results;
        
        try {
            // Fetching the response as a HashMap
            results = restTemplate.getForObject(url, HashMap.class);
            
            
        } catch (HttpClientErrorException e) {
            // Handling errors if the API call fails (e.g., invalid API key, quota exceeded)
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "API call failed: " + e.getMessage());
            return errorResponse.toString();
        }
        int totalResults = (int) results.get("totalResults");

        List<HashMap<String, Object>> result = (List<HashMap<String, Object>>) results.get("articles");
        // Prepare response
        for( HashMap<String, Object> post : result ) {

        	List<String> tempKeywords = new LinkedList<>();
        	HashMap<String, Object> tempPost = new HashMap<>();
            Long articleId = 0l;

            tempPost.put("articleid", articleId);
            tempPost.put("author", null);
            tempPost.put("title", post.get("title") );
            
            String content = post.get("content") != null ? post.get("content").toString() : "";
            String[] splitContent = content.split("…"); // Split at '...' only once
            String description = splitContent[0]; // Take the first part

            // Append the 'Read more...' button
            description += " <button class=\"toggle-button\" style=\"width:auto;\" onclick=\" location.href='" 
            + post.get("url") 
            + "'; \"> Read more... </button> ";
            
            tempPost.put("postComments", null );
            tempPost.put("description", description );
            tempPost.put("likes", 0);
            tempPost.put("dislikes", 0);
            tempPost.put("viewscount", 0);
            tempPost.put("comments", 0);
            try {
                // Parse the ISO 8601 date-time string to a java.util.Date
                Instant instant = Instant.parse( post.get("publishedAt").toString() );
                Timestamp timestamp = Timestamp.from(instant);

                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    tempPost.put("updatedat", formattedDate);
                } else {
                    tempPost.put("updatedat", null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                tempPost.put("updatedat", null);
            }
            tempPost.put("name", post.get("author"));
            tempPost.put("status", "published");
            tempPost.put("username", "tech2xplore");
            tempPost.put("bio", "Tech2Xplore Trending News");
            tempPost.put("category", "News");
            tempPost.put("media", post.get("urlToImage"));
            tempPost.put("image", null);
            tempPost.put("status", "published");
            tempPost.put("isLiked", false);
            tempPost.put("isDisliked", false);
            tempPost.put("disable", true);

            // Fetch keywords for the current article
            for( String keyword: keywords ) {
            	if( 
            			tempPost.get("description").toString().toLowerCase().contains( keyword.toLowerCase() ) 	||
            			tempPost.get("title").toString().toLowerCase().contains( keyword.toLowerCase() )		
            		) {
            		tempKeywords.add( keyword );
            	}
            }
            tempPost.put("keywords", tempKeywords.size() == 0 ? null : tempKeywords );

            if( tempPost.get("name") == null || tempPost.get("name") == "" ) {
            	tempPost.put("name", "Tech2Xplore Trending News");
            }else {
            	tempPost.put("name", tempPost.get("name") + " via Tech2Xplore and News API");
            }
            
            if( 
            		tempPost.get("title").equals("[Removed]") 	||
            		tempPost.get("description").equals("[Removed]") 
            		
            ) {	}else {
            	posts.add( tempPost );
            }
        	
        }
        
        hasMore = hasMore || ( totalResults > page * pageSize );
        
        System.out.print("\n\n\n\n\nPosts : " + posts + "\n\n\n\n");
        Map<String, Object> response = new HashMap<>();
        Long authorId = (Long) request.getSession().getAttribute("authorId");
        
        if( authorId != null && authorId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		response.put("personalImage",null);
        	}else {
        		response.put("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	response.put("personalImage",null);
        }
        
        model.addAttribute("posts", posts);
        model.addAttribute("hasMore", hasMore);

        return "post :: div";
    }

    
    @GetMapping("/runQuery")
    public String boomBaam(){

        jdbcTemplate.execute("""
        		insert into Post (articleid,commentscount,createdat,viewscount,primaryauthor,poststatus,dislikes,likes,publishedat,updatedat,description,postmedia,title) values (62,0,'2025-02-27T08:07:50.330Z',0,8,'published',0,0,'2025-02-27T08:07:50.330Z','2025-02-27T08:07:54.183Z','Humane, which marketed its Ai Pin as the next big thing after smartphones, had raised $240 million from investors, including OpenAI’s Sam Altman. The pin will be discontinued.','post-62.jpg','HP to Buy Parts of Humane, Maker of the Ai Pin, for $116 Million'),(3,0,'2025-02-26T04:22:32.380Z',0,4,'published',0,0,'2025-02-26T04:22:32.380Z','2025-02-26T04:25:43.309Z','Cloud computing has been a game-changer, but are we too dependent on it?Organizations worldwide have embraced cloud platforms for scalability, cost savings, and agility. But here’s the uncomfortable truth—cloud computing isn’t always cheaper or safer.🔴 Hidden Costs of the Cloud:✔ Egress Fees: Moving data out of the cloud can be significantly more expensive than storing it.✔ Vendor Lock-In: Once an enterprise is deeply integrated into AWS, Azure, or GCP, switching providers becomes an expensive nightmare.✔ Security & Compliance Risks: Cloud providers follow strict security protocols, but data breaches and misconfigurations still happen. Just look at recent cloud-based security incidents affecting Fortune 500 companies.So, what’s the way forward?📌 Should enterprises move back to on-prem infrastructure?📌 Can emerging technologies like edge computing reduce cloud dependency?📌 How do we strike a balance between cost, security, and scalability?The conversation around cloud computing is evolving. Let’s rethink the future.','','The Hidden Costs of Cloud Computing: Are We Overlooking the Risks?'),(5,0,'2025-02-26T04:39:29.781Z',0,4,'published',0,0,'2025-02-26T04:39:29.781Z','2025-02-26T04:39:34.571Z','AI is transforming cybersecurity, but here’s the paradox: AI is also making cyberattacks more sophisticated.Cloud security teams now rely on AI-driven threat detection to identify vulnerabilities before hackers exploit them. However, cybercriminals are using AI too—automated malware, AI-driven phishing, and deepfake-based social engineering are making traditional security measures obsolete.🚨 AI-Powered Security Advantages:✔ Real-time threat detection using machine learning✔ Automated response mechanisms to neutralize threats instantly✔ Predictive security analytics to identify potential breaches🔴 But the Risks Are Growing:✔ AI-powered malware can evade detection by constantly mutating✔ Deepfake technology is weaponizing social engineering attacks✔ AI models themselves are vulnerable to adversarial manipulation','',' AI in Cloud Security: Are We Winning or Losing the Cyber War?'),(63,0,'2025-02-27T08:08:13.220Z',0,8,'published',0,0,'2025-02-27T08:08:13.220Z','2025-02-27T08:08:16.719Z','Mira Murati, who left OpenAI last year, has helped establish Thinking Machines Lab, a new artificial intelligence start-up.','post-63.jpg','OpenAI’s Former Chief Technology Officer Starts Her Own Company'),(64,0,'2025-02-27T08:08:28.289Z',0,8,'published',0,0,'2025-02-27T08:08:28.289Z','2025-02-27T08:08:31.189Z','Some tech companies are delaying or pulling their listing plans as the Trump administration’s tariff announcements and other changes cause market volatility and uncertainty.','post-64.jpg','They’ve Been Waiting Years to Go Public. They’re Still Waiting.'),(65,0,'2025-02-27T08:08:40.668Z',0,8,'published',0,0,'2025-02-27T08:08:40.668Z','2025-02-27T08:08:43.572Z','The Finance Committee’s top Democrat sent a letter last month to Dan Morehead, the founder of Pantera Capital, about the investigation.','post-65.jpg','Prominent Cryptocurrency Investor Faces Senate Tax Inquiry'),(67,0,'2025-02-27T08:09:12.904Z',0,8,'published',0,0,'2025-02-27T08:09:12.904Z','2025-02-27T08:09:18.011Z','Bret Taylor, the chairman of OpenAI’s board, said the artificial intelligence company was “not for sale.” Mr. Musk is separately raising money for his A.I. start-up, xAI.','post-67.jpg','OpenAI Rejects Elon Musk’s $97.4 Billion Bid for Control of the Company'),(68,0,'2025-02-27T08:11:44.782Z',0,10,'published',0,0,'2025-02-27T08:11:44.782Z','2025-02-27T08:11:47.808Z','It’s a Valentine’s Day Special!','post-68.jpg','A.I. Accelerates in Paris + Can A.I. Fix Your Love Life?'),(69,0,'2025-02-27T08:12:05.016Z',0,10,'published',0,0,'2025-02-27T08:12:05.016Z','2025-02-27T08:12:09.937Z','Alessio Figalli studies optimal transport, a field of math that ranges from the movements of clouds to the workings of chatbots.','post-69.jpg','A Mathematician Who Makes the Best of Things'),(70,0,'2025-02-27T08:12:25.610Z',0,10,'published',0,0,'2025-02-27T08:12:25.610Z','2025-02-27T08:12:31.640Z','The social media company is attracting investor interest because of Elon Musk’s close ties to President Trump and a recent jump in revenue.','post-70.jpg','Banks Sell $4.7 Billion of X’s Debt, in a Sign of Investor Demand'),(71,0,'2025-02-27T08:12:46.689Z',0,10,'published',0,0,'2025-02-27T08:12:46.689Z','2025-02-27T08:12:51.936Z','The popular social media app was removed to comply with a new law that banned it in the United States. President Trump has paused enforcement of the ban.','post-71.jpg','TikTok Returns to Apple and Google App Stores'),(72,0,'2025-02-27T08:13:06.511Z',0,10,'published',0,0,'2025-02-27T08:13:06.511Z','2025-02-27T08:13:11.468Z','In an email to employees, the company’s chief executive said the company had become bloated during its growth in recent years.','post-72.jpg','Blue Origin, Jeff Bezos’ Rocket Company, Cuts 10% of Its Employees'),(66,1,'2025-02-27T08:08:55.642Z',0,8,'published',0,0,'2025-02-27T08:08:55.642Z','2025-02-27T18:57:36.881Z','The Silicon Valley giant is trying to cut a deal it hopes would help it pull out of a yearslong slump.','post-66.jpg','With Trump’s Help, Intel Could Hand Control of Chip Plants to TSMC'),(1,3,'2025-02-03T23:44:47.023Z',0,1,'published',0,2,'2025-02-03T23:44:47.023Z','2025-02-27T18:58:11.880Z','Spring Boot is an amazing technology. Spring Boot is framework of Spring Boot.','post-1.jpg','Spring Boot'),(6,0,'2025-02-26T04:42:00.249Z',0,4,'published',0,0,'2025-02-26T04:42:00.249Z','2025-02-26T04:42:08.274Z','The days of relying on a single cloud provider are fading.Enterprises are now adopting multi-cloud and hybrid cloud architectures to reduce risks, avoid vendor lock-in, and optimize performance. But this shift brings new challenges in interoperability, security, and cost management.🔹 Multi-Cloud (AWS + Azure + GCP):✔ Prevents vendor lock-in✔ Enables best-in-class services from multiple providers✔ Improves disaster recovery and redundancy🔹 Hybrid Cloud (On-Prem + Cloud):✔ Maintains control over sensitive data✔ Reduces cloud costs by leveraging on-prem infrastructure✔ Enhances security for compliance-driven industriesChallenges:🚨 Interoperability issues: Can AWS and Azure services work together seamlessly?🚨 Security concerns: Managing multiple environments increases complexity.🚨 Performance trade-offs: Which workloads belong on-prem, and which should stay in the cloud?','','The Future of Multi-Cloud & Hybrid Cloud Strategies'),(11,0,'2025-02-26T04:49:58.296Z',0,4,'published',1,0,'2025-02-26T04:49:58.296Z','2025-02-26T07:05:12.717Z','Cloud security is more critical than ever. With data breaches at an all-time high, are we truly securing the cloud, or are we just reacting to threats?🔹 Key Cloud Security Risks:❌ Misconfigurations—One of the biggest causes of data breaches.❌ Insider Threats—Employees with excessive access permissions.❌ API Vulnerabilities—Poorly secured APIs expose cloud environments.💡 Best Practices for Cloud Security:✔ Zero Trust Architecture (ZTA)—Never trust, always verify.✔ Data Encryption—End-to-end encryption for sensitive data.✔ Multi-Factor Authentication (MFA)—Critical for preventing unauthorized access.','','The Truth About Cloud Security: Are We Doing Enough?'),(9,0,'2025-02-26T04:47:23.179Z',0,4,'published',0,0,'2025-02-26T04:47:23.179Z','2025-02-26T04:47:37.212Z','AI is taking over cloud operations—but is this a good thing?AIOps (Artificial Intelligence for IT Operations) is transforming cloud management by enabling:✔ Predictive Maintenance: AI detects issues before they impact performance.✔ Self-Healing Infrastructure: Automated remediation reduces downtime.✔ Anomaly Detection: AI flags security threats in real-time.However, AI-driven cloud automation isn’t perfect:❌ Explainability issues—can we trust AI decisions?❌ Over-reliance on AI—human intervention is still crucial.❌ Security risks—AI-powered cloud systems can be targeted by adversarial attacks.','','The Role of AI in Automating Cloud Operations'),(4,2,'2025-02-26T04:29:55.395Z',0,4,'published',0,3,'2025-02-26T04:29:55.395Z','2025-02-26T07:01:55.700Z','For years, Python has been the de facto language for cloud computing. But now, Rust is gaining traction. So, which one will shape the future of cloud infrastructure?✅ Python: The Powerhouse of AI & Cloud AutomationUnmatched library support (TensorFlow, PyTorch, NumPy, Pandas)Simplifies cloud automation with frameworks like Boto3 for AWSIdeal for AI-driven cloud platforms, thanks to its flexibility and ease of use⚡ Rust: The Rising Star in Cloud Security & PerformanceMemory safety without garbage collection (avoids the pitfalls of C++)Blazing-fast performance for cloud-based microservicesIncreasing adoption in WebAssembly and serverless computingWhile Python dominates AI-driven cloud automation, Rust’s performance and security advantages make it a strong candidate for low-level cloud infrastructure. Companies like Dropbox and Cloudflare have already integrated Rust into their cloud stacks.','',' Python vs. Rust: Which Language Will Dominate Cloud Infrastructure?'),(8,2,'2025-02-26T04:46:07.459Z',0,4,'published',0,2,'2025-02-26T04:46:07.459Z','2025-02-27T18:55:30.770Z','The cloud industry is moving towards serverless computing, where developers focus purely on code while the cloud provider manages the infrastructure. But is serverless truly the future?✅ Benefits of Serverless:✔ No infrastructure management—just deploy functions and let them scale.✔ Cost-effective—pay only for what you use.✔ Ideal for event-driven applications, microservices, and AI workloads.🔴 Challenges:❌ Cold start latency—delays in execution when functions are not pre-warmed.❌ Limited execution time—serverless functions typically have a maximum runtime.❌ Debugging complexity—troubleshooting serverless applications can be tricky.','','Serverless Computing: Hype or Future of Cloud Infrastructure?'),(7,0,'2025-02-26T04:44:46.801Z',0,4,'published',0,1,'2025-02-26T04:44:46.801Z','2025-02-26T06:38:55.878Z','Quantum computing is no longer a distant dream—it’s becoming a reality. Companies like IBM, Google, and AWS are integrating quantum capabilities into their cloud platforms to solve complex problems that traditional computers struggle with.💡 Why Does This Matter?Unlike classical computers that process bits as 0s and 1s, quantum computers use qubits, superposition, and entanglement to handle massive parallel computations. This has major implications for:✔ Cryptography: Post-quantum encryption to secure cloud data.✔ Optimization Problems: Solving logistics, finance, and AI challenges exponentially faster.✔ AI & Machine Learning: Training deep learning models at an unprecedented scale.🔴 Challenges of Quantum Cloud Computing:❌ High error rates due to quantum decoherence.❌ Requires specialized hardware that isn’t widely available.❌ Quantum algorithms are still in their infancy.','','Quantum Computing & The Cloud: The Next Big Revolution?'),(10,2,'2025-02-26T04:48:42.210Z',0,4,'published',2,1,'2025-02-26T04:48:42.210Z','2025-02-27T18:55:32.807Z','Cloud computing has long been the go-to for scalable infrastructure, but edge computing is challenging its dominance.💡 What’s the Difference?✔ Cloud Computing: Centralized data processing in AWS, Azure, or GCP.✔ Edge Computing: Data processed closer to the source—IoT devices, smart cities, autonomous vehicles.🔹 Advantages of Edge Computing:✔ Ultra-low latency—ideal for real-time applications (e.g., self-driving cars).✔ Reduced bandwidth usage—process data locally instead of sending it to the cloud.✔ Improved privacy—keeps sensitive data on-premise.🔴 Challenges:❌ Security vulnerabilities—more devices = larger attack surface.❌ Hardware limitations—edge devices have limited processing power.❌ Integration issues—connecting edge and cloud systems is complex.','','Edge Computing vs. Cloud Computing: Who Will Win?'),(14,1,'2025-02-26T05:45:55.155Z',0,5,'published',0,1,'2025-02-26T05:45:55.155Z','2025-02-26T07:04:12.649Z','Quantum software is powerful but fragile. Security vulnerabilities in quantum cryptography could have catastrophic consequences. Enter Rust.💡 Why Rust for Quantum Computing?✔ Memory Safety: No garbage collection = zero runtime overhead.✔ Concurrency: Rust’s parallelism is ideal for quantum simulations.✔ Security: Built-in ownership model prevents common programming bugs.🔴 Why Rust isn’t Dominating Quantum Yet:❌ Quantum libraries are still evolving.❌ Python dominates quantum frameworks (Qiskit, Cirq, etc.).❌ Rust has a smaller developer pool in the quantum space.🚀 Future Potential:Startups and research labs are already experimenting with Rust for secure quantum applications, especially in cryptography and error correction.','','Rust for Secure Quantum Computing'),(23,0,'2025-02-26T06:04:39.235Z',0,6,'published',1,1,'2025-02-26T06:04:39.235Z','2025-02-26T07:04:58.125Z','C++ has been the king of industrial robotics for decades. From robotic arms on factory floors to autonomous warehouse bots, it provides real-time execution, direct hardware access, and efficiency.✅ Why C++ Still Dominates:Hard real-time performance – essential for tasks like surgical robotics.Extensive libraries (ROS, OpenCV) – widely used in industrial automation.Control over low-level hardware – crucial for embedded systems.But with new languages like Rust gaining traction and AI-powered automation demanding more flexibility, is C++ losing its grip?💡 Some companies are shifting toward AI-driven, Python-based robotic frameworks for prototyping, then optimizing with C++. Could this signal a gradual transition away from C++ dominance?Or will C++ remain the backbone of industrial robotics for the next 50 years?','','The Role of C++ in Industrial Robotics'),(15,1,'2025-02-26T05:47:14.255Z',0,5,'published',0,1,'2025-02-26T05:47:14.255Z','2025-02-26T07:05:04.139Z','AWS, Google, and IBM promise "Quantum Computing as a Service." But how useful are these services today?🔹 Current Quantum Cloud Players:✔ AWS Braket – Access to multiple quantum processors.✔ Google Quantum AI – Sycamore chip, focused on quantum supremacy.✔ IBM Q – One of the most mature public quantum cloud services.🔴 Limitations:❌ Quantum hardware is still in its early stages.❌ High costs for limited qubit access.❌ Not ready for enterprise-scale applications.🚀 What’s Next?Hybrid Quantum + Classical cloud computing is the next step. Companies are working on integrating quantum resources alongside traditional cloud services to maximize performance.','','Quantum Cloud Services: Hype or Reality?'),(24,0,'2025-02-26T06:05:38.281Z',0,6,'published',0,0,'2025-02-26T06:05:38.281Z','2025-02-26T06:05:42.788Z','For years, the debate has raged: Will AI-powered robots replace human workers?The truth is more nuanced. While AI-driven robots are taking over repetitive, dangerous, and highly precise tasks, human workers are still essential for:🔹 Decision-making in uncertain environments🔹 Handling unpredictable variables🔹 Creative problem-solvingCompanies like BMW, Amazon, and Fanuc are deploying collaborative robots (cobots)—AI-enhanced robots that work alongside humans rather than replacing them. These robots assist workers in assembly lines, warehouses, and even surgical procedures.Instead of a robot takeover, we might see a hybrid workforce—where AI augments human skills rather than eliminating them.How do you see AI and robotics shaping the future of work?','','AI & Human Collaboration in Robotics'),(16,0,'2025-02-26T05:48:07.910Z',0,5,'published',0,1,'2025-02-26T05:48:07.910Z','2025-02-26T06:39:35.509Z','AI is revolutionizing cybersecurity, but will it be enough to defend against quantum-powered cyber threats?🔹 AI’s Role in Cybersecurity:✔ Automated Threat Detection – Machine learning identifies cyberattacks in real-time.✔ Self-Healing Systems – AI-driven security protocols adapt to new threats.✔ Behavioral Analysis – AI detects anomalies before they cause damage.🔴 Quantum Hacking Risks:❌ Quantum algorithms can break traditional encryption.❌ AI-powered attacks could outmaneuver current defenses.❌ Cybercriminals will exploit AI-driven security weaknesses.🚀 The Future of AI vs. Quantum Threats:✔ AI-enhanced post-quantum cryptography will be critical.✔ Hybrid AI + Quantum security models will emerge.✔ New ethical challenges will arise as AI automates cyber warfare.','','AI vs. Quantum Cyber Threats'),(25,0,'2025-02-26T06:06:37.239Z',0,6,'published',0,1,'2025-02-26T06:06:37.239Z','2025-02-26T06:40:06.209Z','When it comes to robotics prototyping, speed and flexibility matter more than performance. That’s why most robotics projects start with Python before transitioning to C++ or Rust.Python’s high-level syntax and vast AI libraries (TensorFlow, PyTorch, OpenCV) make it ideal for:✅ AI-powered computer vision✅ Machine learning-driven motion planning✅ Rapid simulation and prototypingBut here’s the catch: Python isn’t built for real-time execution. Once a robot’s logic is proven, it often gets rewritten in C++ or Rust for efficiency.So, while Python accelerates innovation, it may never become the primary language for production-level robotics.🔹 Can AI-powered Python frameworks evolve enough to replace C++ and Rust in real-time robotics? Or will Python remain a prototyping tool?Let’s discuss.','','Python in Prototyping Robotics'),(13,0,'2025-02-26T05:44:42.641Z',0,5,'published',0,1,'2025-02-26T05:44:42.641Z','2025-03-01T01:27:19.135Z','Quantum computers will break RSA and ECC encryption. That’s not a prediction—it’s a fact.💡 The Problem:Most of today’s encryption relies on factorization-based security (RSA, Diffie-Hellman, ECC). However, Shor’s Algorithm—when executed on a large enough quantum computer—will render these obsolete.🔹 Are We Ready?✔ NIST is standardizing post-quantum cryptographic (PQC) algorithms.✔ Lattice-based cryptography (Kyber, Dilithium) is leading the charge.✔ Quantum-resistant blockchains are being explored.🔴 Challenges:❌ Quantum-safe algorithms are computationally expensive.❌ Transitioning from RSA to PQC requires massive infrastructure changes.❌ Quantum attacks might come sooner than expected.🚀 The Future:The race to implement quantum-resistant encryption has begun. The question is—are businesses and governments moving fast enough?','','Post-Quantum Cryptography: Are We Ready?'),(20,0,'2025-02-26T05:54:07.552Z',0,5,'published',1,0,'2025-02-26T05:54:07.552Z','2025-02-26T07:04:40.712Z','Quantum AI promises unparalleled computational power. But are we ready for the ethical implications?🔹 Potential Risks of Quantum AI:✔ Unstoppable AI Decision-Making – Quantum-enhanced AI could surpass human control.✔ Massive Privacy Violations – Quantum computing can break encryption faster than defenses can adapt.✔ Job Displacement – AI-driven automation will accelerate job losses in multiple sectors.🔴 Key Ethical Questions:❌ Should there be global regulations for Quantum AI?❌ Who controls access to quantum AI capabilities?❌ Will superintelligent AI become an existential threat?🚀 Ethical Quantum AI Frameworks Needed:✔ Governments and tech leaders must enforce responsible quantum AI use.✔ Quantum security must be developed alongside quantum AI.✔ Global cooperation is critical to preventing monopolization of quantum AI power.','','The Ethical Dilemma of Quantum AI'),(18,0,'2025-02-26T05:51:34.068Z',0,5,'published',1,0,'2025-02-26T05:51:34.068Z','2025-02-26T07:04:29.888Z','Quantum computing isn’t just about qubits—it also needs classical hardware acceleration. That’s where FPGAs (Field-Programmable Gate Arrays) come in.🔹 Why FPGA Matters for Quantum Computing:✔ Real-time Control: FPGAs handle quantum gate sequencing and error correction.✔ High-Speed Data Processing: Quantum computers still rely on classical pre-processing.✔ Power Efficiency: FPGA-based controllers optimize performance without excessive power use.🔴 Challenges:❌ Limited Programmability – FPGAs require specialized knowledge to optimize.❌ Hardware Constraints – Scaling FPGA-based quantum control systems is complex. ❌ Hybrid Bottlenecks – Communication delays between FPGA and quantum processors exist.🚀 Future of Quantum Hardware Acceleration:✔ More efficient FPGA-based control units for quantum processors.✔ Integration of FPGA with AI to optimize quantum workloads.✔ Development of ASIC-based solutions specifically for quantum computing.','','The Role of FPGA in Quantum Computing'),(19,1,'2025-02-26T05:52:31.758Z',0,5,'published',0,0,'2025-02-26T05:52:31.758Z','2025-02-26T06:46:50.270Z','Wall Street is betting big on quantum finance. But is it ready?🔹 Quantum Computing’s Role in Finance:✔ Faster Portfolio Optimization – Quantum algorithms process risk analysis faster than classical methods.✔ Superior Monte Carlo Simulations – Quantum speedups improve probability modeling.✔ Better Fraud Detection – Quantum AI enhances security monitoring.🔴 Challenges:❌ Financial institutions don’t have quantum-ready infrastructure.❌ Algorithms must be adapted to work within today’s quantum limitations.❌ Quantum-based trading is still in experimental phases.🚀 The Future of Quantum Finance:✔ Early adopters (Goldman Sachs, JPMorgan) are already testing quantum applications.✔ Quantum computers will likely be integrated into high-frequency trading strategies.✔ AI + Quantum models will define next-gen financial risk assessment.','','Quantum Finance: Hype or Reality?'),(22,0,'2025-02-26T06:03:32.550Z',0,6,'published',0,1,'2025-02-26T06:03:32.550Z','2025-02-26T06:39:58.736Z','Automation has always been the holy grail of robotics. But traditional automation was rigid—robots followed pre-defined scripts, unable to adapt in real time.That’s changing with AI-driven robotics. Today, robots are learning from real-world data, improving their tasks without explicit programming. Reinforcement learning, neural networks, and computer vision are enabling robots to:🔹 Self-adjust grip strength when picking up fragile objects🔹 Detect and navigate dynamic obstacles in real time🔹 Improve their assembly line efficiency through AI-driven predictionsTesla’s Optimus humanoid robot, Amazon’s AI-powered warehouse bots, and Boston Dynamics’ AI-enhanced Spot robot show that intelligent automation isn’t just the future—it’s happening now.But as robots become more autonomous, what’s the limit? Should AI-powered robots make independent decisions in high-risk industries? Let’s discuss.','','AI-Driven Automation in Robotics'),(17,0,'2025-02-26T05:50:04.734Z',0,5,'published',0,1,'2025-02-26T05:50:04.734Z','2025-02-26T07:04:27.049Z','AI is evolving fast—but what happens when we add quantum computing into the mix?💡 Quantum Machine Learning (QML) combines AI and quantum computing to process data in ways classical AI cannot.🔹 Why Quantum ML is Revolutionary:✔ Exponential Speedups – Quantum algorithms (e.g., QAOA, VQE) optimize learning models faster.✔ Higher-Dimensional Representations – Quantum states can encode complex datasets efficiently.✔ Better Pattern Recognition – Quantum entanglement can improve feature extraction.🔴 Challenges of QML: ❌ Quantum Hardware is Limited – We still don’t have enough stable qubits.❌ Hybrid Approaches are Needed – Classical AI still does the heavy lifting.❌ No Universal QML Frameworks Yet – Qiskit, TensorFlow Quantum, and PennyLane are evolving but not mature.🚀 Future Outlook:✔ Quantum-enhanced AI models will become the norm.✔ Financial modelling, drug discovery, and optimization problems will benefit first.✔ AI models might soon train on quantum hardware.','','Quantum Machine Learning: A Game Changer?'),(21,0,'2025-02-26T06:02:04.718Z',0,6,'published',1,0,'2025-02-26T06:02:04.718Z','2025-02-26T06:39:50.968Z','For decades, C++ has been the backbone of robotics. It offers raw performance, real-time control, and low-level hardware access. But it also brings memory safety risks—segmentation faults, race conditions, and crashes that can be catastrophic in mission-critical robots.Enter Rust. 🚀Rust’s ownership model and strict memory management eliminate many of the vulnerabilities that plague C++. Robotics companies, including Boston Dynamics and Tesla, are exploring Rust for safety-critical applications.Why Rust is the Future of Robotics:✅ Memory Safety: No null pointers, buffer overflows, or race conditions.✅ Concurrency without Data Races: Essential for multi-threaded robotic systems.✅ Real-Time Performance: No garbage collection pauses, unlike Python.Yet, C++ still dominates real-time robotics. The question is: will Rust eventually replace it, or is it just a niche language for select safety-critical applications?','','Rust in Robotics'),(26,0,'2025-02-26T06:07:42.141Z',0,6,'published',0,0,'2025-02-26T06:07:42.141Z','2025-02-26T06:07:47.757Z','Real-time robotics is one of the hardest problems in AI.Robots don’t just need to make decisions—they need to make instant decisions in dynamic environments. Whether it’s a self-driving car avoiding a pedestrian or a robotic surgeon adjusting mid-procedure, milliseconds matter.🤔 The Challenge:High computational demand – AI inference takes time, but real-time applications can’t afford delays.Network latency – Cloud AI isn’t always fast enough for split-second reactions.Power constraints – Real-time AI consumes massive energy, limiting battery-powered robots.💡 The Solution? Edge Computing.Instead of relying on cloud processing, robots now process AI locally using:✅ On-device neural networks (e.g., NVIDIA Jetson, Tesla’s FSD Chip)✅ FPGA & ASIC accelerators for ultra-fast AI computations✅ Hybrid cloud-edge processing for speed & adaptability🚀 As AI models improve, will we reach true real-time decision-making in robots? Or will processing bottlenecks continue to slow down robotics innovation?','','The Challenges of Real-Time Robotics Processing'),(27,2,'2025-02-26T06:12:37.583Z',0,6,'published',0,2,'2025-02-26T06:12:37.583Z','2025-02-26T06:57:56.533Z','AI-powered robots are incredible—but they consume massive energy. In space missions, disaster zones, or medical robotics, power efficiency is just as important as intelligence.⚡ Why Energy Efficiency Matters:Space exploration: Mars rovers run on limited solar energy. AI efficiency determines mission success.Autonomous drones: Battery life is a limiting factor for UAV operations.Manufacturing robots: Lower energy usage = lower operational costs.💡 Recent Breakthroughs in Energy-Efficient AI:✅ Lightweight AI models – Smaller neural networks, less computation, lower power draw.✅ Neuromorphic computing – Brain-inspired AI chips like IBM’s TrueNorth consume 1/100th the power of GPUs.✅ Self-powered robots – Some bio-inspired robots generate their own energy via kinetic motion or solar harvesting.🔹 If AI can learn to be more energy-efficient, we unlock longer-lasting, more sustainable robotics.Will AI-powered robots soon match biological energy efficiency?','','Energy-Efficient Robotics'),(28,0,'2025-02-26T06:13:37.828Z',0,6,'published',0,0,'2025-02-26T06:13:37.828Z','2025-02-26T06:13:42.182Z','Robots aren’t just for factories and warehouses—they’re now saving lives.From earthquake rescue bots to AI-driven wildfire containment drones, robotics is transforming disaster response.🚁 How AI is Enhancing Disaster Response Robotics:✅ Search-and-rescue drones – AI-powered UAVs scan rubble for survivors (e.g., DJI’s drones used in Turkey’s earthquake relief). ✅ Autonomous firefighting bots – AI-driven machines like Colossus help fight fires in extreme conditions.✅ Underwater rescue robots – AI-powered submersibles assist in deep-sea search and recovery missions.But AI-powered disaster response isn’t perfect.Navigation in extreme conditions is still challenging.AI decision-making under uncertainty is an ongoing problem.🔹 As AI advances, will robots become first responders in every disaster? Or will human expertise always be required for critical decisions?','','AI-Powered Disaster Response Robots'),(35,0,'2025-02-26T06:20:47.950Z',0,6,'published',0,1,'2025-02-26T06:20:47.950Z','2025-02-26T07:04:47.844Z','AI-powered robotics is advancing rapidly, but should we deploy every innovation we create?🛑 Ethical Concerns in AI & Robotics:❌ Job displacement – As robots become more capable, will mass unemployment follow?❌ Bias in AI decision-making – If AI controls hiring, policing, or healthcare, how do we prevent discrimination?❌ AI in warfare – Autonomous drones and AI-controlled weapons raise serious ethical concerns.🔍 How Can We Ensure Ethical AI?✅ Transparent AI development – Open-source AI and regulatory oversight.✅ Human-AI collaboration – AI should assist humans, not replace them entirely.✅ Strict policies on AI weaponization – International laws should regulate AI in military applications.🚀 Final Thought:AI is a tool—it’s up to us to use it responsibly. Will governments step in to regulate AI, or will innovation outpace ethical concerns?','','The Ethics of AI & Robotics: Where Do We Draw the Line?'),(29,1,'2025-02-26T06:14:25.342Z',0,6,'published',0,2,'2025-02-26T06:14:25.342Z','2025-02-26T06:58:07.486Z','Are we truly ready for self-driving cars? 🚘Tesla, Waymo, and Cruise have made huge strides in AI-driven autonomy. Yet, fully self-driving cars still face major hurdles:❌ AI’s inability to handle rare edge cases (e.g., unexpected human behavior)❌ Legal & regulatory barriers – Who’s responsible when AI makes a mistake?❌ Weather & environment limitations – Snow, fog, and poorly marked roads remain challenges.🚀 Recent Breakthroughs in AI for Autonomous Vehicles:✅ End-to-end deep learning models – AI that learns from human drivers directly.✅ Sensor fusion techniques – Combining LiDAR, cameras, and radar for better situational awareness.✅ V2X (Vehicle-to-Everything) communication – Cars talking to other cars, traffic lights, and road sensors.Despite these advances, are humans ready to fully trust AI with their lives?','','AI in Autonomous Vehicles'),(30,0,'2025-02-26T06:15:27.494Z',0,6,'published',0,1,'2025-02-26T06:15:27.494Z','2025-02-26T07:04:51.519Z','🚀 Where is AI-driven robotics headed?Over the last decade, we’ve seen massive advancements in automation, AI, and robotic intelligence. But what’s next?🔹 Near Future (5-10 years):✅ Smarter AI-powered assistants (warehouse robots, personal AI companions)✅ Enhanced human-robot collaboration in industrial settings✅ Greater autonomy in robotics with real-time AI🔹 Mid-Term (10-20 years):✅ AI-driven humanoid robots that can operate in human environments✅ Autonomous construction (robots building homes, roads, infrastructure)✅ AI-powered medical robotic assistants in hospitals🔹 Long-Term (20+ years):✅ Fully self-learning robots that improve without human programming✅ Robotic colonization of space (NASA’s AI-powered robots on Mars and beyond)✅ AI-powered robots replacing dangerous human jobs (deep-sea mining, nuclear cleanup)Will AI-driven robotics surpass human capabilities, or will we always need humans in the loop?The next decade will define the answer.','','The Future of AI-Driven Robotics'),(54,0,'2025-02-27T08:00:33.466Z',0,11,'published',0,0,'2025-02-27T08:00:33.466Z','2025-02-27T08:00:37.821Z','The end of a court fight with the largest U.S. crypto company would be a big win for an industry that financially backed President Trump.','post-54.jpg','Coinbase Says S.E.C. Will Drop Crypto Lawsuit'),(34,0,'2025-02-26T06:20:06.485Z',0,6,'published',0,0,'2025-02-26T06:20:06.485Z','2025-02-26T06:20:09.790Z','🤖 Will AI replace workers, or will we see true human-robot collaboration?Despite concerns about AI taking jobs, some experts believe that the future of robotics lies in augmentation, not replacement.🛠 Key Areas of Human-Robot Collaboration:✅ Manufacturing – AI-powered cobots (collaborative robots) work alongside factory workers.✅ Surgery – AI-assisted robotic surgery improves precision while keeping human doctors in control.✅ Logistics – AI-driven robots handle repetitive warehouse tasks, letting humans focus on complex decision-making.🚀 How AI is Enhancing Human Jobs Instead of Replacing Them:🔹 AI-powered exoskeletons assist construction workers in lifting heavy objects.🔹 Warehouse cobots speed up order fulfillment without eliminating human jobs.🔹 Robotic assistants in hospitals help nurses by automating routine tasks.👀 The Big Question:Are we entering an age of true human-AI collaboration, or will automation eventually replace most jobs?','','Can Humans & AI Work Together in Robotics?'),(31,0,'2025-02-26T06:17:00.889Z',0,6,'published',0,1,'2025-02-26T06:17:00.889Z','2025-02-26T06:40:19.176Z','Are humanoid robots the future of labor?Companies like Tesla (Optimus), Agility Robotics (Digit), and Boston Dynamics are pushing humanoid robots into real-world applications. These robots can walk, lift objects, and interact with environments designed for humans—making them viable for industries like logistics, healthcare, and even customer service.💡 Why Humanoid Robots Matter:✅ Adapting to Human Spaces – Unlike wheeled robots, humanoids can work in warehouses, hospitals, and offices.✅ Labor Shortages – Countries with aging populations (e.g., Japan) are testing humanoids for caregiving roles.✅ Industrial Efficiency – Robots like Digit can work alongside humans in warehouses, handling repetitive tasks.🚧 Challenges Still Exist:❌ Balance & Dexterity – Unlike humans, robots struggle with stability and delicate tasks.❌ Energy Efficiency – Humanoids consume high amounts of power, limiting their operational time.❌ Cost – These robots are still expensive to produce and maintain.Will humanoid robots become as common as factory automation, or are they still just a futuristic experiment?','','The Rise of Humanoid Robots in the Workforce'),(33,0,'2025-02-26T06:19:25.574Z',0,6,'published',0,1,'2025-02-26T06:19:25.574Z','2025-02-26T07:04:50.043Z','The construction industry is one of the least automated—but that’s changing.📌 Why Construction Needs Robots:🏗️ Labour shortages – Skilled workers are declining, delaying projects.🔨 Repetiitive, dangerous work – Robots can handle hazardous joobs.⏳ Faster project completion – AI-powered machines can work non-stop.🚀 Game-Changing Robotics in Construction:✅ 3D Printing Robots – Companies like ICON are printing entire houses in less than 24 hours.✅ Autonomous Excavators – AI-driven machinery like Built Robotics’ self-operating bulldozers is reshaping job sites.✅ Bricklaying Robots – Robots like SAM100 lay bricks 6 times faster than humans.🚧 Challenges Ahead:❌ High upfront costs – Robotics investment is expensive for smaller firms.❌ Regulations & safety concerns – New laws are needed to integrate robots safely.❌ Complex environments – Unlike factories, construction sites constantly change, making AI adaptation difficult.Will robots revolutionize construction, or is human labour too vital to replace?','','Robots in Construction: The Future of Smart Infrastructure?'),(53,0,'2025-02-27T08:00:14.773Z',0,11,'published',0,0,'2025-02-27T08:00:14.773Z','2025-02-27T08:00:19.299Z','The company said a Chinese operation had built the tool to identify anti-Chinese posts on social media services in Western countries.','post-53.jpg','OpenAI Uncovers Evidence of A.I.-Powered Chinese Surveillance Tool'),(55,0,'2025-02-27T08:00:51.234Z',0,11,'published',0,0,'2025-02-27T08:00:51.234Z','2025-02-27T08:00:54.760Z','“Elon Musk is willing to spend a phenomenal amount of money and basically do everything he can to stay with the head of the pack on A.I. progress.”','post-55.jpg','How ‘Based’ Is Grok 3? + Robinhood C.E.O. Vlad Tenev on Markets for Everything + Vibecoding 101'),(56,0,'2025-02-27T08:01:09.603Z',0,11,'published',0,0,'2025-02-27T08:01:09.603Z','2025-02-27T08:01:15.647Z','Tech start-ups typically raised huge sums to hire armies of workers and grow fast. Now artificial intelligence tools are making workers more productive and spurring tales of “tiny team” success.','post-56.jpg','A.I. Is Changing How Silicon Valley Builds Start-Ups'),(57,0,'2025-02-27T08:01:29.656Z',0,11,'published',0,0,'2025-02-27T08:01:29.656Z','2025-02-27T08:01:32.821Z','Mr. Musk, one of President Trump’s main advisers, has not outlined a plan to reverse falling sales at the electric car company of which he is chief executive.','post-57.jpg','Does Elon Musk Still Care About Selling Cars?'),(58,0,'2025-02-27T08:01:48.157Z',0,11,'published',0,0,'2025-02-27T08:01:48.157Z','2025-02-27T08:01:52.464Z','The president’s company, Trump Media & Technology Group, represents a clear mingling of his official duties and his business interests.','post-58.jpg','With Truth Social, Trump Has Official Mouthpiece and a Channel for Revenue'),(59,0,'2025-02-27T08:02:10.334Z',0,11,'published',0,0,'2025-02-27T08:02:10.334Z','2025-02-27T08:02:17.308Z','Microsoft’s new “topological qubit” is not based on a solid, liquid or gas. It is another phase of matter that many experts did not think was possible.','post-59.jpg','Microsoft Says It Has Created a New State of Matter to Power Quantum Computers'),(60,0,'2025-02-27T08:07:20.074Z',0,8,'published',0,0,'2025-02-27T08:07:20.074Z','2025-02-27T08:07:25.085Z','The company, which once enjoyed a surging stock price, struggled to turn its plans for electric and hydrogen trucks into a viable business.','post-60.jpg','Nikola, E.V. Start-Up That Once Thrilled Investors, Files for Bankruptcy'),(61,0,'2025-02-27T08:07:35.223Z',0,8,'published',0,0,'2025-02-27T08:07:35.223Z','2025-02-27T08:07:38.108Z','How did a successful, financially sophisticated banker gamble his community’s money away?','post-61.jpg','The Cryptocurrency Scam That Turned a Small Town Against Itself'),(32,1,'2025-02-26T06:18:13.374Z',0,6,'published',0,1,'2025-02-26T06:18:13.374Z','2025-02-26T06:48:31.896Z','How do robots learn? Traditionally, programmers define every movement. But with Reinforcement Learning (RL), robots can self-learn behaviors—just like humans.📌 What is RL?Reinforcement Learning allows robots to:✅ Trial-and-error learning – The robot tries different actions and improves over time.✅ Adapt to new environments – AI models adjust dynamically, making them more flexible.✅ Optimize for efficiency – AI finds the most efficient way to complete a task.🏆 Real-World Uses of RL in Robotics:🚗 Self-driving cars – AI learns to navigate complex environments.🤖 Industrial automation – Robots improve picking and sorting tasks without constant reprogramming.⚽ AI-powered robots in sports – RL is training robots to play soccer, like in the RoboCup competition.🚨 But RL Has Its Challenges:❌ Training takes time – AI requires millions of simulations to learn.❌ Safety concerns – Robots learning by trial-and-error can make dangerous mistakes.❌ Computation-heavy – RL demands high processing power (GPUs, TPUs).Will RL-trained robots redefine automation, or is it still too slow for real-world deployment?','','Reinforcement Learning in Robotics: The Next AI Breakthrough?'),(36,0,'2025-02-26T22:08:07.615Z',0,9,'published',0,0,'2025-02-26T22:08:07.615Z','2025-02-27T00:42:32.581Z','1. Problem-Solving SkillsCoding teaches how to break down complex problems into smaller, more manageable parts. It encourages logical thinking, creativity, and persistence in finding solutions.2. Job OpportunitiesWith the growing reliance on technology in nearly every industry, coding skills are highly sought after in fields such as software development, data analysis, web development, and artificial intelligence. This opens up a wide range of career opportunities.3. Creativity and InnovationCoding allows you to create new things, whether that’s building a website, developing an app, or designing a game. It provides a platform for personal creativity and innovation.','','Benefits of Daily Coding '),(37,0,'2025-02-27T00:35:11.095Z',0,9,'published',0,0,'2025-02-27T00:35:11.095Z','2025-02-27T00:42:35.345Z','WASHINGTON On January 20, 2025, Travis Hill became Acting Chairman of the Federal Deposit Insurance Corporation (FDIC).  Acting Chairman Hill issued the following statement: It is my honor and privi','','Statement from Acting Chairman Travis Hill - FDIC'),(38,0,'2025-02-27T01:00:12.835Z',0,9,'published',0,0,'2025-02-27T01:00:12.835Z','2025-02-27T01:00:13.811Z','Anthropic is releasing Claude 3.7 Sonnet, its first “hybrid reasoning model” that can solve more complex problems and outperforms previous models in areas like math and coding.  In addition to a new model, Anthropic is also releasing a “limited research previ','','Anthropic’s new ‘hybrid reasoning’ AI model is its smartest yet'),(39,0,'2025-02-27T01:00:17.037Z',0,9,'published',0,0,'2025-02-27T01:00:17.037Z','2025-02-27T01:00:17.888Z','A free version of Gemini Code Assist, Google’s enterprise-focused AI coding tool, is now available globally for solo developers. Google announced today that Gemini Code Assist for individuals is launching in public preview, aiming to make coding assistants “w','','Google Gemini’s AI coding tool is now free for individual users'),(40,0,'2025-02-27T01:00:19.874Z',0,9,'published',0,0,'2025-02-27T01:00:19.874Z','2025-02-27T01:00:21.314Z','The chatbot is part of Elon Musk and President Donald Trump’s ambitions to use AI and other technologies to cut costs and modernize the US government.','','Elon Musk’s DOGE Is Working on a Custom Chatbot Called GSAi'),(45,0,'2025-02-27T07:56:09.169Z',0,11,'published',0,0,'2025-02-27T07:56:09.169Z','2025-02-27T07:56:12.330Z','The electric-car maker’s stock has had a bumpy ride since the victory of President Trump, who has given Tesla’s chief, Elon Musk, a role in Washington.','post-45.jpg','Tesla Shares Fall 8% as Post-Election Surge Dissipates'),(46,0,'2025-02-27T07:57:31.560Z',0,11,'published',0,0,'2025-02-27T07:57:31.560Z','2025-02-27T07:57:35.570Z','The company said it was working to fix the problem after iPhone users began reporting the issue.','post-46.jpg','Apple’s Dictation System Transcribes the Word ‘Racist’ as ‘Trump’'),(41,0,'2025-02-27T01:04:59.899Z',0,9,'published',0,0,'2025-02-27T01:04:59.899Z','2025-02-27T07:50:47.607Z','In the age of artificial intelligence, entry-level coders are doomed. But some engineers are thriving.','post-41.jpg','The AI coding apocalypse'),(42,0,'2025-02-27T01:05:04.462Z',0,9,'published',0,0,'2025-02-27T01:05:04.462Z','2025-02-27T07:50:48.185Z','Bundle a lifetime license to Microsoft Visual Studio Pro 2022 with the Premium Learn to Code Certification Courses from StackSocial—now 97% off.','post-42.jpg','A $1,999 Coding Bundle for Just $49.97? Microsoft Visual Studio and Code Certification Courses Deal Is Here'),(43,0,'2025-02-27T01:05:07.938Z',0,9,'published',0,0,'2025-02-27T01:05:07.938Z','2025-02-27T07:50:48.568Z','Seasoned engineers and people with zero coding experience are embracing "vibe coding" — the act of relying on AI to write code for them.','post-43.jpg','Silicon Valley''s next act: bringing vibe coding to the world'),(44,0,'2025-02-27T01:11:27.162Z',0,9,'published',0,0,'2025-02-27T01:11:27.162Z','2025-02-27T07:50:48.933Z','Amazon is aiming to catch up in generative artificial intelligence and to reboot its virtual assistant, which has been leapfrogged by powerful chatbots.','post-44.jpg','Amazon Unveils Alexa+, Powered by Generative A.I.'),(47,0,'2025-02-27T07:58:23.993Z',0,11,'published',0,0,'2025-02-27T07:58:23.993Z','2025-02-27T07:58:28.998Z','The company pledged the multibillion-dollar investment over the next four years and said it would create 20,000 jobs. The Texas facility is set to open in 2026.','post-47.jpg','Apple Vows to Build A.I. Servers in Houston and Spend $500 Billion in U.S.'),(48,0,'2025-02-27T07:58:41.952Z',0,11,'published',0,0,'2025-02-27T07:58:41.952Z','2025-02-27T07:58:44.843Z','The Silicon Valley company, which dominates the market for chips needed to build A.I. systems, said revenue was up 78 percent from a year earlier.','post-48.jpg','Nvidia’s Profit Jumps 80% as Company Rides Tech’s A.I. Boom'),(49,0,'2025-02-27T07:58:57.623Z',0,11,'published',0,0,'2025-02-27T07:58:57.623Z','2025-02-27T07:59:01.717Z','The company reached the lunar surface in 2024, and now its second lander aims to improve on the feat. Three other spacecraft also hitched a ride on the SpaceX rocket.','post-49.jpg','Intuitive Machines’ Athena Lander Launches on Journey to the Moon'),(50,0,'2025-02-27T07:59:13.549Z',0,11,'published',0,0,'2025-02-27T07:59:13.549Z','2025-02-27T07:59:17.529Z','The dream of mining metals in deep space crashed and burned in the 2010s. AstroForge’s Odin mission to survey a potentially metallic asteroid is packed and ready to lift off.','post-50.jpg','Earth’s 1st Asteroid Mining Prospector Heads to the Launchpad'),(51,0,'2025-02-27T07:59:33.510Z',0,11,'published',0,0,'2025-02-27T07:59:33.510Z','2025-02-27T07:59:39.302Z','Hours after Coinbase said the S.E.C. was dropping a lawsuit against it, another major cryptocurrency exchange reported a potentially record-setting theft.','post-51.jpg','Big Day for Crypto Goes South in a Hurry After a Giant Hack'),(52,0,'2025-02-27T07:59:53.184Z',0,11,'published',0,0,'2025-02-27T07:59:53.184Z','2025-02-27T07:59:58.699Z','Law enforcement in the country was pressuring the company to create a tool that would act like a back door into customers’ data.','post-52.jpg','Under Government Pressure, Apple Pulls Security Feature in Britain'),(73,0,'2025-02-27T03:55:33.941Z',0,7,'published',0,0,'2025-02-27T03:55:33.941Z','2025-02-27T03:55:46.539Z','I is revolutionizing cybersecurity—but it’s also empowering hackers.🚨 How AI is Changing the Game:✅ AI-Powered Cyber Defense – Machine learning models can detect anomalous behavior and predict cyber threats.✅ Automated Threat Response – AI-driven SOC (Security Operations Center) automation is reducing response times.✅ Phishing Detection – AI scans emails and websites to detect fraudulent activity in real time.⚠️ Buut Here’s the Problem:Hackers are using AI too:❌ AI-Generated Malwarre – Adapts to evade antivirus detection.❌ Deepfake Phishing – AI creates voice/video deepfakes to trick users into giving access.❌ Automated Hacking Bots – AI speeds up brute-force attacks and credential stuffing.🚀 What’s the Solution?🔹 AI vs. AI Security – Security experts are developing defensive AI to counter cybercriminal AI.🔹 Behavioral AI Detection – Monitoring unusual activity patterns instead of just known threats.🔹 Cybersecurity Awareness – The best first line of defense is an informed user base.💬 Is AI in cybersecurity a net positive, or are we in an arms race we can’t win?','','Python & AI-Powered Cybersecurity: A Double-Edged Sword'),(74,0,'2025-02-27T03:58:17.990Z',0,7,'published',0,0,'2025-02-27T03:58:17.990Z','2025-02-27T03:58:24.168Z','Phishing is evolving—and AI is making it terrifyingly realistic.📌 Deepfake-powered attacks are changing the game:🔹 Fake CEO calls – Employees receive realistic AI-generated voice messages from their "boss" requesting urgent wire transfers.🔹 Video-based scams – Attackers create deepfake Zoom meetings to steal credentials.🔹 Personalized spear phishing – AI analyzes social media to craft ultra-convincing phishing emails.🚨 Real-World Example:In 2020, a UK energy firm lost $243,000 after scammers used deepfake AI to mimic their CEO’s voice and authorize a fraudulent bank transfer.🔐 How Do We Defend Against This?✅ Voiceprint authentication – Using AI to detect synthetic voices.✅ Behavior-based security – Analyzing user interactions beyond voice and video.✅ Employee training – Raising awareness about deepfake threats.🔍 Question for You:Are companies taking deepfake phishing seriously enough, or will AI scams spiral out of control?','','AI & Phishing Attacks: The Deepfake Problem'),(75,0,'2025-02-27T03:59:09.283Z',0,7,'published',0,0,'2025-02-27T03:59:09.283Z','2025-02-27T03:59:13.599Z','Traditional cybersecurity operates on trust—but that’s outdated.Zero-Trust Security (ZTS) removes implicit trust and forces verification at every step.📌 Why is this Critical for AI Systems?🚨 AI-powered attacks are getting more sophisticated. If an attacker breaches one system, they can move laterally across networks undetected.💡 Zero-Trust Fixes This:✅ Verify every request – No automatic access, even inside the network.✅ Least privilege access – Users and devices get only the permissions they need.✅ Continuous authentication – AI monitors behavior for anomalies.📉 Real-World Example:In 2021, a major ransomware attack crippled a U.S. pipeline because a single compromised VPN password gave full access. Zero-trust policies could have prevented it.🚀 Your Take?Are businesses ready to adopt zero-trust security, or is it still seen as too complex to implement?','','Zero-Trust Security in AI-Powered Systems'),(76,0,'2025-02-27T04:00:13.276Z',0,7,'published',0,0,'2025-02-27T04:00:13.276Z','2025-02-27T04:00:17.274Z','🔴 Cybercrime is becoming a business.Hackers no longer need elite skills—they can now buy ransomware toolkits online and launch attacks within hours.📌 What is Ransomware-as-a-Service (RaaS)?It’s organized cybercrime where criminals sell ready-made ransomware to less-skilled attackers in exchange for a cut of the ransom.🚨 Why is RaaS Exploding?💰 Low barrier to entry – Anyone can launch a ransomware attack.💳 Anonymous crypto payments – Attackers demand Bitcoin, making tracking difficult.🎯 Easy target selection – Hackers target hospitals, schools, and small businesses that can’t afford downtime.📉 Shocking Stats:🔹 Ransomware costs will hit $265 billion by 2031.🔹 Attacks happen every 2 seconds worldwide.🔹 60% of businesses close within 6 months after a ransomware attack.🔐 How Do We Stop RaaS?✅ Endpoint detection & response (EDR) – AI-driven solutions to stop ransomware before encryption.✅ Backup best practices – Keeping offline, immutable backups.✅ No-ransom policies – Governments are considering banning ransom payments to deter attacks.💬 Your Thoughts?Should companies refuse to pay ransoms, or is that unrealistic in a crisis?','','The Rise of Ransomware-as-a-Service (RaaS)'),(77,0,'2025-02-27T04:01:42.702Z',0,7,'published',0,0,'2025-02-27T04:01:42.702Z','2025-02-27T04:01:52.902Z','AI-powered cybersecurity tools promise to stop attacks before they happen—but do they really work?📌 What AI Does Well:✅ Anomaly Detection – AI can detect unusual patterns in real time.✅ Predictive Analysis – Machine learning can forecast potential breaches.✅ Automated Response – AI-based systems can isolate infected machines instantly.⚠️ But Here’s the Problem:❌ False Positives – AI flags too many legittimate activvities as threats.❌ Adversarial AI – Attackers can trick AI into ignoring real threats.❌ Lack of Context – AI struggles with understanding business risks vs. minor anomalies.💡 The Verdict:AI improves threat detection, but human expertise is still essential. AI doesn’t replace cybersecurity teams—it augments them.🔍 Your Thoughts?Will AI ever reach 100% reliable cybersecurity, or will human oversight always be required?','','AI in Threat Detection: Hype or Reality?'),(78,0,'2025-02-27T04:02:25.145Z',0,7,'published',0,0,'2025-02-27T04:02:25.145Z','2025-02-27T04:02:28.358Z','Quantum computers could break today’s encryption—but should we panic now?📌 The Problem:🔹 RSA-2048, ECC, and Diffie-Hellman are vulnerable to quantum attacks.🔹 Shor’s algorithm could crack current cryptography in minutes once large-scale quantum computers arrive.🔹 Encrypted data stolen today might be decrypted in the future—a "harvest now, decrypt later" risk.🚀 The Solution:✅ Post-Quantum Cryptography (PQC) – New encryption algorithms resist quantum attacks.✅ Hybrid Encryption – Mixing classical + quantum-resistant methods for safe transitions.✅ Early Adoption – Governments (like the NSA) are already mandating PQC research.🔍 Key Question:Are organizations taking quantum threats seriously, or is post-quantum security still on the backburner?','','🚀 Post-Quantum Cryptography: Urgent or Overhyped?'),(79,0,'2025-02-27T04:03:18.178Z',0,7,'published',0,0,'2025-02-27T04:03:18.178Z','2025-02-27T04:03:22.048Z','To stop hackers, you have to think like one.🚀 What Ethical Hacking Brings to the Table:✅ Red Team vs. Blue Team Drills – Simulating real cyberattacks.✅ Bug Bounties – Paying hackers to find vulnerabilities before criminals do.✅ Zero-Day Hunting – Identifying unknown weaknesses in systems.📉 Real-World Example:In 2022, ethical hackers discovered a critical vulnerability in Tesla’s autopilot, preventing potential remote takeovers.💡 Should Every Cybersecurity Team Include Hackers?Some companies hesitate, fearing trust issues—but can we afford not to have them?','','Ethical Hacking: The Best Cyber Defense?'),(80,0,'2025-02-27T04:03:52.769Z',0,7,'published',0,0,'2025-02-27T04:03:52.769Z','2025-02-27T04:03:56.591Z','📉 Alarming Stat:By 2025, there will be 3.5 million unfilled cybersecurity jobs worldwide.🚨 Why Are We Facing a Talent Crisis?❌ High Skill Barriers – Companies demand years of experience, even for entry-level roles.❌ Fast-Changing Threats – New hacking techniques emerge faster than training can keep up.❌ Burnout & Stress – Cybersecurity jobs involve high pressure and constant emergencies.🚀 Solutions:✅ Hands-on Training Programs – Focus on real-world hacking simulations instead of just theory.✅ AI-Assisted Security Teams – Reducing manual workload with AI automation.✅ Lowering Entry Barriers – Hiring based on skills, not just degrees & certifications.💡 How Can We Fix This?Should cybersecurity recruit more ethical hackers or focus on AI-driven automation to fill the gap?','','Cybersecurity Talent Shortage'),(81,0,'2025-02-27T04:04:30.510Z',0,7,'published',0,0,'2025-02-27T04:04:30.510Z','2025-02-27T04:04:33.468Z','💡 What Will Cybersecurity Look Like in 2030?🚀 Predictions:✅ AI-Only Cyberattacks – Hackers will deploy fully autonomous AI viruses.✅ Self-Healing Systems – AI-based defenses will repair vulnerabilities in real time.✅ Biometric-Based Security – Passwords will be replaced with behavioral AI and brainwave authentication.⚠️ Biggest Threats on the Horizzon:❌ AI-Generated Malware – Smmart viruses that change signatures in real time.❌ Quantum Decryption – Quantum computers may break encryption instantly.❌ Automated Social Engineering – AI phishing bots impersonating humans flawlessly.🔍 The Big Question:Will AI make cybersecurity impossible to breach or impossible to defend?','','The Future of AI & Cybersecurity'),(82,0,'2025-02-27T04:05:50.502Z',0,7,'published',0,0,'2025-02-27T04:05:50.502Z','2025-02-27T04:05:53.465Z','💰 Fraud is getting smarter. Is AI smart enough to stop it?🔍 The Challenge:Financial cybercrime is becoming more sophisticated, with AI-powered fraudsters using deepfakes, automated scams, and real-time manipulation.🚀 How AI Helps:✅ Transaction Monitoring – Detects unusual spending patterns.✅ Behavioral Biometrics – Tracks typing speed, cursor movements, and more.✅ Deep Learning Models – Analyzes millions of fraud patterns in seconds.⚠️ The Problem:❌ False Positives – AI caan flag legitimate transactions, frustratinng customers.❌ Adversarial AI – Cybercriminals are training AI to bypass AI.❌ Privacy Concerns – Some AI fraud systems intrude on user data.💡 The Future:🔹 Will AI and cybersecurity always be in an arms race?🔹 Should banks rely entirely on AI, or do human analysts still matter?','','AI-Powered Fraud Detection: Can It Stop Financial Cybercrime?'),(83,0,'2025-02-27T04:06:25.571Z',0,7,'published',0,0,'2025-02-27T04:06:25.571Z','2025-02-27T04:06:28.648Z','🔓 The easiest way to hack a system? Trick the human behind it.📌 Why Social Engineering Works:✅ It’s Psychological, Not Technical – People trust emails, calls, and messages from familiar sources.✅ It Exploits Urgency – "Your bank account is locked! Click this link now."✅ It’s Evolving – AI-generated deepfake voices now impersonate CEOs and executives.🚨 Recent Example:🔹 In 2023, a multinational company lost $25 million after scammers used AI-generated voices to impersonate an executive on a Zoom call.🔍 How to Defend Against It:🔹 Zero Trust Policies – Always verify, even if the request looks real.🔹 AI-Based Behavioral Analysis – Identify unusual communication patterns.🔹 Cybersecurity Training – Teach employees to spot social engineering tactics.💡 Question for You:With AI deepfakes becoming indistinguishable from reality, how can companies verify identities in 2025 and beyond?','','Social Engineering: The Biggest Cybersecurity Weakness'),(12,1,'2025-02-26T05:42:02.274Z',0,5,'published',1,2,'2025-02-26T05:42:02.274Z','2025-02-27T18:55:38.564Z','C++ is making a quiet yet powerful resurgence in the world of quantum computing. While Python dominates quantum frameworks (Qiskit, Cirq, PennyLane), C++ is proving to be the hidden giant when it comes to performance-intensive quantum simulations.🔹 Why C++ Matters in Quantum Computing?✔ Speed & Efficiency: Quantum systems require low-latency operations.✔ Memory Management: Unlike Python, C++ gives fine-grained control over memory, crucial for hardware-level quantum simulations.✔ Parallel Processing: Quantum workloads thrive on multithreading and concurrency, which C++ handles better than interpreted languages.🔴 Challenges of Using C++ in Quantum Computing:❌ Complexity: C++ has a steep learning curve compared to Python.❌ Less Community Support: Most quantum libraries are Python-first.❌ Development Time: Quantum algorithms require rapid prototyping—Python excels here.','','C++ in Quantum Computing: The Hidden Giant'),(84,0,'2025-03-01T02:58:37.418Z',0,5,'published',0,0,'2025-03-01T02:58:37.418Z','2025-03-01T03:00:16.831Z','🕹 Can blockchain change gaming forever?📌 What Web3 Gaming Offers:✅ Ownership of Assets – Buy, sell, trade NFTs (Axie Infinity, Decentraland).✅ Play-to-Earn (P2E) – Earn real money from in-game rewards.✅ Interoperability – Transfer items across games.⚠️ Challenges:  ❌ P2E Sustainability – Many models collapse (e.g., Axie Infinity’s crash). ❌ High Entry Barriers – Expensive NFTs limit players.❌ Scalability Issues – Blockchain gaming still lags behind traditional games.🔹 Is Web3 gaming the future, or just a speculative bubble?','','Blockchain in Gaming: The Play-to-Earn Revolution'),(85,1,'2025-03-01T09:32:23.100Z',0,8,'published',0,1,'2025-03-01T09:32:23.100Z','2025-03-01T09:34:22.940Z','A junior dev wrote this:============================int sum = 0;for (int i = 0; i < numbers.length; i++) {  sum += numbers[i];}System.out.println(sum);=============================I refactored it to: : ======================================System.out.println(Arrays.stream(numbers).sum());======================================💡 Less code, same logic!Keep it simple, keep it clean.What’s the best refactor you’ve done? 🔥#Coding #BestPractices #JavaTips','','Don''t Overcomplicate Your Code! '),(2,3,'2025-02-26T04:09:42.800Z',0,1,'published',1,1,'2025-02-26T04:09:42.800Z','2025-03-04T02:17:29.328Z','The days of relying on a single cloud provider are fading.Enterprises are now adopting multi-cloud and hybrid cloud architectures to reduce risks, avoid vendor lock-in, and optimize performance. But this shift brings new challenges in interoperability, security, and cost management.🔹 Multi-Cloud (AWS + Azure + GCP):✔ Prevents vendor lock-in✔ Enables best-in-class services from multiple providers✔ Improves disaster recovery and redundancy🔹 Hybrid Cloud (On-Prem + Cloud):✔ Maintains control over sensitive data✔ Reduces cloud costs by leveraging on-prem infrastructure✔ Enhances security for compliance-driven industriesChallenges:🚨 Interoperability issues: Can AWS and Azure services work together seamlessly?🚨 Security concerns: Managing multiple environments increases complexity.🚨 Performance trade-offs: Which workloads belong on-prem, and which should stay in the cloud?C++ and Java remain legacy favorites for enterprise cloud applications, but Python and Rust are emerging as the preferred languages for multi-cloud automation.📌 Is multi-cloud the future, or just an unnecessary complexity?📌 Will hybrid cloud dominate regulated industries?📌 What’s the best way to manage a cloud-agnostic infrastructure?Let’s discuss—what’s your cloud strategy?','','The Future of Multi-Cloud & Hybrid Cloud Strategies');	
		""");
        System.out.println("Done h");
    	
    	return "Query ";
    }
    
    /* @GetMapping("/truncate")
    public String truncateTables(){
    	
    	List<String> tables = jdbcTemplate.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

    	for (String table : tables) {
            // Drop table if exists
            jdbcTemplate.execute("TRUNCATE TABLE " + table + " CASCADE;");
            System.out.println("Table " + table + " has been dropped.");
        }
    	
    	return "show";
    }    
    *//*
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
    
    *//*
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
    
    *//*
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
            
//            jdbcTemplate.execute("""
//        	    DO $$
//        	    BEGIN
//        	        IF NOT EXISTS (
//        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_blogger'
//        	        ) THEN
//        	            CREATE TRIGGER set_updated_at_blogger
//        	            BEFORE UPDATE ON Blogger
//        	            FOR EACH ROW
//        	            EXECUTE FUNCTION update_updated_at_column();
//        	        END IF;
//
//        	        IF NOT EXISTS (
//        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_post'
//        	        ) THEN
//        	            CREATE TRIGGER set_updated_at_post
//        	            BEFORE UPDATE ON Post
//        	            FOR EACH ROW
//        	            EXECUTE FUNCTION update_updated_at_column();
//        	        END IF;
//
//        	        IF NOT EXISTS (
//        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_category'
//        	        ) THEN
//        	            CREATE TRIGGER set_updated_at_category
//        	            BEFORE UPDATE ON Category
//        	            FOR EACH ROW
//        	            EXECUTE FUNCTION update_updated_at_column();
//        	        END IF;
//        	        
//        	        IF NOT EXISTS (
//        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_keyword'
//        	        ) THEN
//        	            CREATE TRIGGER set_updated_at_keyword
//        	            BEFORE UPDATE ON Keyword
//        	            FOR EACH ROW
//        	            EXECUTE FUNCTION update_updated_at_column();
//        	        END IF;
//
//        	        IF NOT EXISTS (
//        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_communities'
//        	        ) THEN
//        	            CREATE TRIGGER set_updated_at_communities
//        	            BEFORE UPDATE ON Community
//        	            FOR EACH ROW
//        	            EXECUTE FUNCTION update_updated_at_column();
//        	        END IF;
//
//        	        IF NOT EXISTS (
//        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_keyword_assignments'
//        	        ) THEN
//        	            CREATE TRIGGER set_updated_at_keyword_assignments
//        	            BEFORE UPDATE ON KeywordAssignment
//        	            FOR EACH ROW
//        	            EXECUTE FUNCTION update_updated_at_column();
//        	        END IF;
//
//        	        IF NOT EXISTS (
//        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_post_category_assignments'
//        	        ) THEN
//        	            CREATE TRIGGER set_updated_at_post_category_assignments
//        	            BEFORE UPDATE ON PostCategoryAssignment
//        	            FOR EACH ROW
//        	            EXECUTE FUNCTION update_updated_at_column();
//        	        END IF;
//
//        	        IF NOT EXISTS (
//        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_memberships'
//        	        ) THEN
//        	            CREATE TRIGGER set_updated_at_memberships
//        	            BEFORE UPDATE ON Membership
//        	            FOR EACH ROW
//        	            EXECUTE FUNCTION update_updated_at_column();
//        	        END IF;
//
//        	        IF NOT EXISTS (
//        	            SELECT 1 FROM pg_trigger WHERE tgname = 'set_updated_at_collaboration'
//        	        ) THEN
//        	            CREATE TRIGGER set_updated_at_collaboration
//        	            BEFORE UPDATE ON Collaboration
//        	            FOR EACH ROW
//        	            EXECUTE FUNCTION update_updated_at_column();
//        	        END IF;
//        	    END;
//        	    $$;
//        	"""); 

// (1,'2025-03-19T11:47:13.483Z','2025-04-16T15:24:07.270Z','active','{}','$2a$10$fp5nqs.KlxBYXkHCTi5Ituy.Ejf8udQ/kKYUQoJnEQko7waFTaoUi','','I am Developer','krish','krish@gmail.com','krish')
           
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
            
            /*
             * Now not needed cause
             * Backup Script is created

            Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM Category
            """,Long.class);
            
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
            """,Long.class);
            
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
            / * /

            // Add triggers to tables with `updated_at` column
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
            """);	//


            
            return "redirect:/";
            
    } 
    
    *//*
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
    }	*/

    
    
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
    public String register(Model model, String name, String username, String email, String password, String confirmPassword, String bio, MultipartFile image, HttpServletRequest request) {

        // Check if the user already exists (by email or username)
        String checkUserSql = "SELECT COUNT(*) FROM Blogger WHERE email = ? OR username = ?";
        Long existingUserCount = jdbcTemplate.queryForObject(checkUserSql, Long.class, email, username);

        if (existingUserCount != null && existingUserCount > 0) {
            model.addAttribute("error", "Username or Email already exists!");
            return "redirect:/register?error=true"; // Return to registration page with error message
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match!");
            return "redirect:/register?error=true";
        }

        // Generate a new author ID
        String authorIdSql = "SELECT COALESCE(MAX(authorId) + 1, 1) FROM Blogger";
        Long newAuthorId = jdbcTemplate.queryForObject(authorIdSql, Long.class);


        String uploadedImagePath = "";

        if (image!= null && !image.isEmpty()) {
            try {
                // Define the path to save the image
                String uploadDir = bloggerStoreDirectory;
                String fileName = image.getOriginalFilename();
                
                // Ensure the directory exists
                File directory = new File(uploadDir);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                
                // Save the file
                String[] parts = fileName.split("\\.");
                String extension = parts[parts.length - 1];
                
                uploadedImagePath = "blogger-" + String.valueOf( newAuthorId ) + "." + extension;
                // Construct the media file name
                System.out.println("Media: " + uploadedImagePath);

                System.out.print("ID : " + String.valueOf(newAuthorId));
                
                Path filePath = Paths.get(uploadDir, uploadedImagePath);
                Files.write(filePath, image.getBytes());

                System.out.print("File : " + uploadedImagePath);
                
            } catch (IOException e) {
                return "redirect:/register?error=true";
            }
        }
        
        // Secure password hashing using PasswordEncoder
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(password);

        // Insert the new user into the database
        String insertUserSql = """
            INSERT INTO Blogger (authorId, name, username, email, password, bio, profilePicture, createdAt, updatedAt) 
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;

        int rowsAffected = jdbcTemplate.update(insertUserSql, newAuthorId, name, username, email, hashedPassword, bio, uploadedImagePath);

        request.getSession().setAttribute("authorId", newAuthorId);

        model.addAttribute("loggedInUser", (Long) request.getSession().getAttribute("authorId") ); // Add the logged-in username

        if (rowsAffected > 0) {

            return "redirect:/register?success=true"; // Redirect to login with success flag
        } else {
            model.addAttribute("error", "Registration failed. Please try again.");
            return "redirect:/register?error=true";
        }

    }

    
    @PostMapping("/contact")
    public String submitSuggestion(HttpServletRequest request, Model model, @RequestParam("type") String type, @RequestParam("message") String message) {
    	
        if( (Long) request.getSession().getAttribute("authorId") == null ) {
        	System.out.print(request.getSession().toString());
        	return "redirect:/login";
        }
        
        // Insert the new user into the database
        String insertUserSql = """
            INSERT INTO Suggestion (authorId, feedbackType, message, createdAt ) 
            VALUES ( ?,  CAST(? AS feedback_type_enum) , ?, CURRENT_TIMESTAMP)
        """;
        
        if (this.userExist != null && !this.userExist.isEmpty()) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }

        int rowsAffected = jdbcTemplate.update(insertUserSql, (Long) request.getSession().getAttribute("authorId") , type, message);

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
                    "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                    "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                    "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                    "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                    "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                    "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                    "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                    "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                    "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"
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
    	
    	this.userExist = null;
    	model.addAttribute("loggedInUser", null); // No user logged in
    	request.getSession().setAttribute("authorId", null);
    	
        if (this.userExist != null && !this.userExist.isEmpty()) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
    	
    	if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
    	return "redirect:/logout?success=true";
    }
    
    @GetMapping("/mass-filter/{entity}/{target}")
    public String showMassFilterPage(Model model, @PathVariable String entity, @PathVariable String target, HttpServletRequest request ) {
        // Fetch categories
        String categorySql = "SELECT name FROM Category";
        List<String> categoriesForTags = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("categories", categoriesForTags);

        // Fetch keywords
        String keywordSql = "SELECT name FROM Keyword";
        List<String> keywordsForTags = jdbcTemplate.queryForList(keywordSql, String.class);
        model.addAttribute("keywords", keywordsForTags);
        
        Long authorId = (Long) request.getSession().getAttribute("authorId");
        
        if( authorId != null && authorId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		model.addAttribute("personalImage",null);
        	}else {
        		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	model.addAttribute("personalImage",null);
        }
        
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
            List<Long> categories = jdbcTemplate.queryForList(sql, Long.class, "%" + target + "%");

            if (categories.isEmpty()) {
                model.addAttribute("error", "No posts found in this category.");
                model.addAttribute("posts", null);
                return "filter"; 
            }

            Long categoryId = categories.get(0);

            // Query to retrieve posts along with keywords
            sql = """
                    SELECT p.articleid, 
                       p.primaryAuthor AS author, 
                    p.title, 
                    p.description, 
                    p.likes, 
                    p.dislikes, 
                    p.viewscount, 
                    p.commentscount AS comments, 
                    p.updatedat, 
                    p.postmedia AS media,
                    p.poststatus AS status,
                    u.name AS name, 
                    u.username AS username, 
                    u.bio AS bio, 
                    u.profilepicture AS image,
                    c.name AS category
	             FROM Post p 
	             JOIN Blogger u ON p.primaryAuthor = u.authorid 
	             JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
	             JOIN Category c ON pca.categoryid = c.categoryid
	             WHERE c.categoryid = ? 
	         """;

            posts = jdbcTemplate.query(sql, (rs, rowNum) -> {
	             Map<String, Object> post = new HashMap<>();
	             Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
	             
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
	             post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
	             post.put("title", rs.getString("title"));
	             post.put("disable",false);
	             post.put("description", rs.getString("description"));
	             post.put("likes", rs.getInt("likes"));
	             post.put("dislikes", rs.getInt("dislikes"));
	             post.put("viewscount", rs.getInt("viewscount"));
	             post.put("comments", rs.getInt("comments"));
	             Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
	             post.put("name", rs.getString("name"));
	             post.put("username", rs.getString("username"));
	             post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
	             post.put("category", rs.getString("category"));
	             if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
	             if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}
	
	             // Separate query to get keywords for the current article
	             String keywordQuery = """
	             		SELECT name FROM Keyword k 
	             		JOIN KeywordAssignment ka 
	             		ON k.keywordid = ka.keywordid 
	             		WHERE ka.articleid = ?
	             	""";
	         	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
	         	post.put("keywords", keywords);
	

	        	if( authorId != null && authorId > 0 ) {

	        		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
	        		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	        	
	            	post.put("isLiked", isReact == 0 ? false : true);
	            	
	            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
	            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	            	
	            	post.put("isDisliked", isReact == 0 ? false : true );
	        	
	        	}else {
	        		post.put("isLiked", false );            		
	        		post.put("isDisliked", false );
	        	}
	         	
	             return post;
	         }, categoryId);
       	
        }
        
        if( entity.equals("%keyword%") ) {
        	
        	sql = "SELECT keywordId FROM Keyword WHERE LOWER(name) LIKE LOWER(?)";
            List<Long> keywords = jdbcTemplate.queryForList(sql, Long.class, "%" + target + "%");

            if (keywords.isEmpty()) {
                model.addAttribute("error", "No posts found in this category.");
                return "filter"; // Return with error message if no category found
            }

            Long keywordId = keywords.get(0);

            // Fetch posts based on keyword ID
            sql = """
                SELECT 
                    p.articleid, 
                       p.primaryAuthor AS author, 
                    p.title, 
                    p.description, 
                    p.likes, 
                    p.dislikes, 
                    p.viewscount, 
                    p.commentscount AS comments, 
                    p.updatedat, 
                    p.postmedia AS media,
                    p.poststatus AS status,
                    u.name AS name, 
                    u.username AS username, 
                    u.bio AS bio, 
                    u.profilepicture AS image,
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
	                         p.viewscount, p.commentscount, p.updatedat, 
	                         u.name, u.username, u.bio, u.profilepicture, c.name
	          """;

            posts = jdbcTemplate.query(sql, (rs, rowNum) -> {
            	Map<String, Object> post = new HashMap<>();
                Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
                
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
                post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
                post.put("title", rs.getString("title"));
                post.put("disable",false);
                post.put("description", rs.getString("description"));
                post.put("likes", rs.getInt("likes"));
                post.put("dislikes", rs.getInt("dislikes"));
                post.put("viewscount", rs.getInt("viewscount"));
                post.put("comments", rs.getInt("comments"));
                Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
                post.put("name", rs.getString("name"));
                post.put("username", rs.getString("username"));
                post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
                post.put("category", rs.getString("category"));
                if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
                if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

                // Separate query to get keywords for the current article
                String keywordQuery = """
                		SELECT name FROM Keyword k 
                		JOIN KeywordAssignment ka 
                		ON k.keywordid = ka.keywordid 
                		WHERE ka.articleid = ?
                	""";
            	List<String> keywordsOfPost = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
            	post.put("keywords", keywordsOfPost);
            	

            	if( authorId != null && authorId > 0 ) {

            		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
            		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
                	post.put("isLiked", isReact == 0 ? false : true);
                	
                	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
                	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
                	
                	post.put("isDisliked", isReact == 0 ? false : true );
            	
            	}else {
            		post.put("isLiked", false );            		
            		post.put("isDisliked", false );
            	}
            	
            	return post;
            } ,keywordId);
        	
        }

        model.addAttribute("posts", posts);
        
    	List<String> colors = new ArrayList<>(
			List.of(
                "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"
            )
		);
    	
    	model.addAttribute("topic", this.capitalize(target.substring(1,target.length() - 1)) ); 
    	model.addAttribute("colors", colors); 
        
        return "massfilter";
    }
    
    @GetMapping("/mission")
    public ResponseEntity<?> mission( HttpServletRequest request) {

    	try {
    		
    		ArrayList<Long> ids = new ArrayList<Long>(
    				List.of(41L, 42L, 43L, 44L)
				);
    		
    		for( Long userId : ids ) {
	    		String fileName = "post-" + userId + ".jpg";
	    		
		    	String sql = "UPDATE Post SET postmedia = ? WHERE articleid = ?";
		        jdbcTemplate.update(sql, fileName, userId);
		
		        System.out.print(sql + " " + fileName + " " + userId );
    		}
	        return ResponseEntity.ok(Map.of("message", "Profile picture updated successfully"));
	    } catch (Exception e ) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to save profile picture"));
	    }
    	
    }
    	
    @PostMapping("/update-picture")
    public ResponseEntity<?> updateProfilePicture(@RequestParam("profilePicture") MultipartFile file, HttpServletRequest request) {
        Long userId = (Long) request.getSession().getAttribute("authorId");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not authenticated"));
        }

        String fileName = file.getOriginalFilename();
        String[] parts = fileName.split("\\.");
        String extension = parts[parts.length - 1];

        // Define the directory for saving uploaded 
        String uploadDir = bloggerStoreDirectory;
        fileName = "blogger-" + userId + "." + extension;


        try {
            // Save the file
            Path path = Paths.get(uploadDir, fileName);

            if( Files.exists(path)) {
//            	Files.delete(path);
            }

            try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE)) {
                os.write(file.getBytes());
            }catch(IOException e) {
            	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to save profile picture"));            	
            }

            // Update user's profile picture path in the database
            String sql = "UPDATE Blogger SET profilePicture = ? WHERE authorId = ?";
            jdbcTemplate.update(sql, fileName, userId);

            System.out.print(sql + " " + fileName + " " + userId );
            
            return ResponseEntity.ok(Map.of("message", "Profile picture updated successfully"));
        } catch (Exception e ) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to save profile picture"));
        }
    }
    
    @PostMapping("/profile")
    public ResponseEntity<Map<String, String>> update(@RequestBody String profileData, HttpServletRequest request ) {
    	
    	Long authorId = (Long) request.getSession().getAttribute("authorId");
        String jsonPart = profileData;
        
        String field = jsonPart.substring(jsonPart.indexOf("\"field\":\"") + 9, jsonPart.indexOf("\",\"value\""));
        String value = jsonPart.substring(jsonPart.indexOf("\"value\":\"") + 9, jsonPart.lastIndexOf("\""));
        
        // Print extracted field and value
        System.out.println("Field: " + field);
        System.out.println("Value: " + value);
        
    	boolean success = false;
    	
    	if( authorId == null || authorId < 1 ) {
    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    	}
    	
    	if( field.equals("password") ) {
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            value = passwordEncoder.encode(value);
    	}
    	
    	String sql = """
    		    UPDATE Blogger
    		    SET %s = ?
    		    WHERE authorId = ?
    		""";

    		// Dynamically format the SQL to include the field (sanitize it if user-provided)
    		String formattedSql = String.format(sql, field);

    		// Execute the update with value and authorId
    		int rowsAffected = jdbcTemplate.update(formattedSql, value, authorId);

    		// Check if the update was successful
    		success = rowsAffected > 0; // Simplified boolean expression

    		System.out.println("Update success: " + success);
    	
        Map<String, String> responseMap = new HashMap<>();
    	if (success) {
    	    responseMap.put(field, value);  // Assuming data[0] and data[1] are valid keys and values
    	    return ResponseEntity.ok(responseMap);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    
    @GetMapping("/entity")
    public String hideEntityPage(Model model) {

        String categorySql = "SELECT name FROM Category";
        List<String> categoriesForTags = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("categories", categoriesForTags);

        // Fetch keywords
        String keywordSql = "SELECT name FROM Keyword";
        List<String> keywordsForTags = jdbcTemplate.queryForList(keywordSql, String.class);
        model.addAttribute("keywords", keywordsForTags);
        
        Map<String,Object> entity = new HashMap<String,Object>();
        
        entity.put("type", "");
        entity.put("title", "");
        entity.put("media", "");
        entity.put("description", "");	
        entity.put("hasPosts", null);	
        
    	List<String> colors = new ArrayList<>(
    			List.of(
    			    "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                    "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                    "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                    "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                    "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                    "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                    "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                    "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                    "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"                )
    		);
    	
        if (this.userExist != null && !this.userExist.isEmpty()) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        	
    	model.addAttribute("topic", null ); 
    	model.addAttribute("colors", colors); 
    	model.addAttribute("entity", entity);
    	model.addAttribute("news","Select The Entity Please");
        
        return "entity";
    }
    
    @GetMapping("/entity/{entity}/{target}")
    public String showEntityPage(Model model, @PathVariable String entity, @PathVariable String target, HttpServletRequest request ) {
    	
    	Map<String, Object> entityInfo = new HashMap<>();
    	entityInfo.put("hasPosts", true);	

        String categorySql = "SELECT name FROM Category";
        List<String> categoriesForTags = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("categories", categoriesForTags);

        // Fetch keywords
        String keywordSql = "SELECT name FROM Keyword";
        List<String> keywordsForTags = jdbcTemplate.queryForList(keywordSql, String.class);
        model.addAttribute("keywords", keywordsForTags);
        
        Long authorId = (Long) request.getSession().getAttribute("authorId");
        
        if( authorId != null && authorId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, authorId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		model.addAttribute("personalImage",null);
        	}else {
        		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	model.addAttribute("personalImage",null);
        }
        
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

        	entityInfo.put("type", "category");
        	
        	sql = "SELECT name, categoryIcon AS media, categoryDescription AS description FROM Category WHERE LOWER(name) LIKE LOWER(?)";
        	
        	List<Map<String, Object>> temp = jdbcTemplate.queryForList(sql, target);
        	
        	entityInfo.put("title", this.capitalize( temp.get(0).get("name").toString() ) );
        	entityInfo.put("media", temp.get(0).get("media") );
        	entityInfo.put("description", temp.get(0).get("description") );
        	
        	
        	sql = "SELECT categoryId FROM Category WHERE LOWER(name) LIKE LOWER(?)";
            List<Long> categories = jdbcTemplate.queryForList(sql, Long.class, "%" + target + "%");

            if (categories.isEmpty()) {
                model.addAttribute("error", "No posts found in this category.");
                model.addAttribute("posts", null);
                return "filter"; 
            }

            Long categoryId = categories.get(0);

            // Query to retrieve posts along with keywords
            sql = """
                    SELECT p.articleid, 
                       p.primaryAuthor AS author, 
                    p.title, 
                    p.description, 
                    p.likes, 
                    p.dislikes, 
                    p.viewscount, 
                    p.commentscount AS comments, 
                    p.updatedat, 
                    p.postmedia AS media,
                    p.poststatus AS status,
                    u.name AS name, 
                    u.username AS username, 
                    u.bio AS bio, 
                    u.profilepicture AS image,
                    c.name AS category
	             FROM Post p 
	             JOIN Blogger u ON p.primaryAuthor = u.authorid 
	             JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
	             JOIN Category c ON pca.categoryid = c.categoryid
	             WHERE c.categoryid = ? 
	         """;

            posts = jdbcTemplate.query(sql, (rs, rowNum) -> {
	             Map<String, Object> post = new HashMap<>();
	             Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
	             
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
	             post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
	             post.put("title", rs.getString("title"));
	             post.put("disable",false);
	             post.put("description", rs.getString("description"));
	             post.put("likes", rs.getInt("likes"));
	             post.put("dislikes", rs.getInt("dislikes"));
	             post.put("viewscount", rs.getInt("viewscount"));
	             post.put("comments", rs.getInt("comments"));
	             Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
	             post.put("name", rs.getString("name"));
	             post.put("username", rs.getString("username"));
	             post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
	             post.put("category", rs.getString("category"));
	             if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
	             if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}
	
	             // Separate query to get keywords for the current article
	             String keywordQuery = """
	             		SELECT name FROM Keyword k 
	             		JOIN KeywordAssignment ka 
	             		ON k.keywordid = ka.keywordid 
	             		WHERE ka.articleid = ?
	             	""";
	         	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
	         	post.put("keywords", keywords);
	

	        	if( authorId != null && authorId > 0 ) {

	        		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
	        		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	        	
	            	post.put("isLiked", isReact == 0 ? false : true);
	            	
	            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
	            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	            	
	            	post.put("isDisliked", isReact == 0 ? false : true );
	        	
	        	}else {
	        		post.put("isLiked", false );            		
	        		post.put("isDisliked", false );
	        	}
	         	
	             return post;
	         }, categoryId);
       	
        }
        
        if( entity.equals("%keyword%") ) {
        	
        	entityInfo.put("type", "keyword");
        	
        	sql = "SELECT name, keywordIcon AS media, keywordDescription AS description FROM Keyword WHERE LOWER(name) LIKE LOWER(?)";
        	
        	List<Map<String, Object>> temp = jdbcTemplate.queryForList(sql, target);
        	
        	entityInfo.put("title", this.capitalize( temp.get(0).get("name").toString() ) );
        	entityInfo.put("media", temp.get(0).get("media") );
        	entityInfo.put("description", temp.get(0).get("description") );
        	
        	sql = "SELECT keywordId FROM Keyword WHERE LOWER(name) LIKE LOWER(?)";
            List<Long> keywords = jdbcTemplate.queryForList(sql, Long.class, "%" + target + "%");

            if (keywords.isEmpty()) {
                model.addAttribute("error", "No posts found in this category.");
                return "filter"; // Return with error message if no category found
            }

            Long keywordId = keywords.get(0);

            // Fetch posts based on keyword ID
            sql = """
                SELECT 
                    p.articleid, 
                       p.primaryAuthor AS author, 
                    p.title, 
                    p.description, 
                    p.likes, 
                    p.dislikes, 
                    p.viewscount, 
                    p.commentscount AS comments, 
                    p.updatedat, 
                    p.postmedia AS media,
                    p.poststatus AS status,
                    u.name AS name, 
                    u.username AS username, 
                    u.bio AS bio, 
                    u.profilepicture AS image,
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
	                         p.viewscount, p.commentscount, p.updatedat, 
	                         u.name, u.username, u.bio, u.profilepicture, c.name
	          """;

            posts = jdbcTemplate.query(sql, (rs, rowNum) -> {
            	Map<String, Object> post = new HashMap<>();
                Long articleId = rs.getLong("articleid");
                Long tempAuthorId = rs.getLong("author");
                
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
                post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
                post.put("title", rs.getString("title"));
                post.put("disable",false);
                post.put("description", rs.getString("description"));
                post.put("likes", rs.getInt("likes"));
                post.put("dislikes", rs.getInt("dislikes"));
                post.put("viewscount", rs.getInt("viewscount"));
                post.put("comments", rs.getInt("comments"));
                Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = sdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
                post.put("name", rs.getString("name"));
                post.put("username", rs.getString("username"));
                post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
                post.put("category", rs.getString("category"));
                if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
                if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

                // Separate query to get keywords for the current article
                String keywordQuery = """
                		SELECT name FROM Keyword k 
                		JOIN KeywordAssignment ka 
                		ON k.keywordid = ka.keywordid 
                		WHERE ka.articleid = ?
                	""";
            	List<String> keywordsOfPost = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
            	post.put("keywords", keywordsOfPost);
            	

            	if( authorId != null && authorId > 0 ) {

            		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
            		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
                	post.put("isLiked", isReact == 0 ? false : true);
                	
                	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
                	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
                	
                	post.put("isDisliked", isReact == 0 ? false : true );
            	
            	}else {
            		post.put("isLiked", false );            		
            		post.put("isDisliked", false );
            	}
            	
            	return post;
            } ,keywordId);
        	
        }

        model.addAttribute("posts", posts);
        
    	List<String> colors = new ArrayList<>(
			List.of(
                "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"	
            )
		);
    	
    	model.addAttribute("topic", this.capitalize(target.substring(1,target.length() - 1)) ); 
    	model.addAttribute("entity", entityInfo); 
    	model.addAttribute("colors", colors); 

    	return "entity";
    }
    
    /* @PostMapping("/reaction/{reaction}/{articleid}")
    public ResponseEntity<Map<String, Object>> performReaction(
            @PathVariable String reaction,
            @PathVariable String articleid,
            HttpServletRequest request) {

        // Parse the article ID
        Long articleId;
        try {
            articleId = Long.valueOf(articleid);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid article ID"));
        }

        // Retrieve the author ID from the session
        Long authorId = (Long) request.getSession().getAttribute("authorId");
        if (authorId == null) {
        	System.out.println("User Without Login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not authenticated"));
        }

        try {
        	String sql = "SELECT COUNT(*) AS count, reactiontype FROM PostInteraction WHERE articleId = ? AND authorId = ? GROUP BY reactiontype";

        	// Use a Map to fetch multiple columns
        	Map<String, Object> results = jdbcTemplate.queryForMap(sql, articleId, authorId);

        	// Extract the values
        	Long result = (Long) results.get("count");
        	String oldReaction = results.get("reactiontype") != null ? results.get("reactiontype").toString() : null ;
            
        	if ("nil".equalsIgnoreCase(reaction) ) {
                // Remove the reaction if "nil" is passed
                sql = "DELETE FROM PostInteraction WHERE articleId = ? AND authorId = ?";
                jdbcTemplate.update(sql, articleId, authorId);
                
                sql = "UPDATE Post SET " + oldReaction + "s = " + oldReaction + "s - 1 WHERE articleId = ? AND primaryauthor = ? ";
                jdbcTemplate.update(sql, articleId, authorId);
                System.out.println("Reaction Removed " + " for " + sql);
                
            } else if (result == null || result == 0) {
                // Insert a new reaction if no previous reaction exists
                sql = """
                    INSERT INTO PostInteraction (createdAt, articleId, authorId, reactionType)
                    VALUES (CURRENT_TIMESTAMP, ?, ?, '""" +
                     reaction.toLowerCase() +
                    "')";
                jdbcTemplate.update(sql, articleId, authorId);
                
                sql = "UPDATE Post SET " + reaction.toLowerCase() + "s = " + reaction.toLowerCase() + "s + 1 WHERE articleId = ? AND primaryauthor = ? ";
                jdbcTemplate.update(sql, articleId, authorId);
                
                System.out.println("Reaction Inserted As " + reaction + " for " + sql);
            } else {
                // Update the reaction type if a previous reaction exists
                sql = """
                		UPDATE PostInteraction SET reactionType = '""" +
                     reaction.toLowerCase() +
                    "' WHERE articleId = ? AND authorId = ?";
                jdbcTemplate.update(sql, articleId, authorId);
                
                sql = "UPDATE Post SET " + reaction.toLowerCase() + "s = " + reaction.toLowerCase() + "s + 1, " + oldReaction + "s = " + oldReaction + "s - 1 "  + "WHERE articleId = ? AND primaryauthor = ? ";
                jdbcTemplate.update(sql, articleId, authorId);
                
                System.out.println("Reaction Updated As " + reaction + " for " + sql);
            }
        } catch (Exception e) {
        	System.out.println("Error Occured");
        	e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An error occurred: " + e.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Reaction processed successfully"));
    }	*/
    
    @PostMapping("/reaction/{reaction}/{articleid}")
    public ResponseEntity<Map<String, Object>> performReaction(
            @PathVariable String reaction,
            @PathVariable String articleid,
            HttpServletRequest request) {

        // Parse the article ID
        Long articleId;
        try {
            articleId = Long.valueOf(articleid);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid article ID"));
        }

        // Retrieve the author ID from the session
        Long authorId = (Long) request.getSession().getAttribute("authorId");
        if (authorId == null) {
            System.out.println("User Without Login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not authenticated"));
        }

        try {
            // Query to check the user's existing reaction
            String sql = """
                SELECT COUNT(*) AS count, reactiontype 
                FROM PostInteraction 
                WHERE articleId = ? AND authorId = ? 
                GROUP BY reactiontype
            """;

            // Fetch results; use try-catch for empty result scenarios
            Map<String, Object> results = null;
            Long result = 0L;
            String oldReaction = null;

            try {
                results = jdbcTemplate.queryForMap(sql, articleId, authorId);
            } catch (Exception e) {
                // Log the exception for debugging, but proceed with defaults
                System.out.println("No existing reaction found or query failed: " + e.getMessage());
            }

            // Safely extract values from the results
            if (results != null) {
                result = (Long) results.getOrDefault("count", 0L);
                oldReaction = (String) results.get("reactiontype");
            }


            if ("nil".equalsIgnoreCase(reaction)) {
                // Remove the reaction if "nil" is passed
                sql = "DELETE FROM PostInteraction WHERE articleId = ? AND authorId = ?";
                jdbcTemplate.update(sql, articleId, authorId);

                if (oldReaction != null) {
                    sql = "UPDATE Post SET " + oldReaction + "s = GREATEST(" + oldReaction + "s - 1, 0) WHERE articleId = ?";
                    jdbcTemplate.update(sql, articleId);
                }

                System.out.println("Reaction Removed");
            } else if (result == 0) {
                // Insert a new reaction if no previous reaction exists
                sql = """
                    INSERT INTO PostInteraction (createdAt, articleId, authorId, reactionType)
                    VALUES (CURRENT_TIMESTAMP, ?, ?, '%s')
                """.formatted(reaction.toLowerCase());
                jdbcTemplate.update(sql, articleId, authorId);

                sql = "UPDATE Post SET " + reaction.toLowerCase() + "s = " + reaction.toLowerCase() + "s + 1 WHERE articleId = ?";
                jdbcTemplate.update(sql, articleId);

                System.out.println("Reaction Inserted As " + reaction);
            } else {
                // Update the reaction type if a previous reaction exists
                sql = """
                    UPDATE PostInteraction 
                    SET reactionType = '%s' 
                    WHERE articleId = ? AND authorId = ?
                """.formatted(reaction.toLowerCase());
                jdbcTemplate.update(sql, articleId, authorId);

                if (oldReaction != null && (!oldReaction.equalsIgnoreCase(reaction)) ){
                    sql = """
                        UPDATE Post 
                        SET %s = %s + 1, %s = GREATEST(%s - 1 , 0)
                        WHERE articleId = ?
                    """.formatted(
                        reaction.toLowerCase() + "s",
                        reaction.toLowerCase() + "s",
                        oldReaction.toLowerCase() + "s",
                        oldReaction.toLowerCase() + "s"
                    );
                    jdbcTemplate.update(sql, articleId);
                }

                System.out.println("Reaction Updated As " + reaction);
            }
        } catch (Exception e) {
            System.out.println("Error Occurred");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Reaction processed successfully"));
    }

    @PostMapping("/connection/{action}/{to}")
    public ResponseEntity<Map<String, Object>> addConnection(
            @PathVariable String action,
            @PathVariable String to,
            HttpServletRequest request) {

    	Long authorId = (Long) request.getSession().getAttribute("authorId");
        if (authorId == null) {
            System.out.println("User Without Login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not authenticated"));
        }
        
        Long toBlogger = Long.valueOf(to);
        if (toBlogger == null) {
            System.out.println("Blogger To Not Exist");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "User not authenticated"));
        }
        
        if (toBlogger == authorId) {
            System.out.println("Blogger and Follower Should Not Be Same");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "User not authenticated"));
        }
        
        String sql;
        
        if( action.equals("follow") ) {
        	 
        	sql = "SELECT COUNT(*) AS count FROM Connection WHERE followerId = "
        		 + authorId + " AND followingId = " + toBlogger + ";";
        	
        	Long count = jdbcTemplate.queryForObject(sql, Long.class);
        	
        	if( count == 0 ) {
	        	 sql = """
	        	 		INSERT INTO Connection(followerId, followingId, connectionStatus, createdAt)
	        	 		VALUES(
	        	 	""" + authorId +  // its like author is follower
	        	 	"," + toBlogger   + // and following this is to that means following
	        	 	", 'accepted' ,CURRENT_TIMESTAMP)";
	        	 jdbcTemplate.update(sql);
        	}
        }
        
        else if( action.equals("unfollow") ) {
        	
        	sql = """
                    DELETE FROM Connection
                    WHERE followerId = 
                  """ + authorId + " AND followingId = " + toBlogger+ ";"
                  .formatted("rejected");
        	
        	 jdbcTemplate.update(sql); 
        }
        
        else {
        	return ResponseEntity.status(HttpStatus.OK).
        			body(Map.of("message", "Different Message"));
        	
        }
        System.out.print(sql);
        
        
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", this.capitalize(action) + " processed successfully"));
    }

    @PostMapping("/comment/{articleId}")
    public ResponseEntity<?> postComment(
    		@PathVariable Long articleId, 
    		@RequestBody Map<String, String> commentRequest,
    		HttpServletRequest request){
        // Validate input
    	String message = commentRequest.get("message");
    	boolean curse = false;
    	
    	ArrayList<String> curseWords = new ArrayList<String>(
    		List.of(
				"arse", "arsehead", "arsehole", "ass", "ass hole", "asshole", "bastard", "bitch", "bloody", "bollocks", "brotherfucker", "bugger", "bullshit", "child-fucker", "Christ on a bike", "Christ on a cracker", "cock", "cocksucker", "crap", "cunt", "dammit", "damn", "damned", "damn it", "dick", "dick-head", "dickhead", "dumb ass", "dumb-ass", "dumbass", "dyke", "faggot", "father-fucker", "fatherfucker", "fuck", "fucked", "fucker", "fucking", "god dammit", "goddammit", "God damn", "god damn", "goddamn", "Goddamn", "goddamned", "goddamnit", "godsdamn", "hell", "holy shit", "horseshit", "in shit", "jackarse", "jack-ass", "jackass", "Jesus Christ", "Jesus fuck", "Jesus Harold Christ", "Jesus H. Christ", "Jesus, Mary and Joseph", "Jesus wept", "kike", "mother fucker", "mother-fucker", "motherfucker", "nigga", "nigra", "pigfucker", "piss", "prick", "pussy", "shit", "shit ass", "shite", "sibling fucker", "sisterfuck", "sisterfucker", "slut", "son of a bitch", "son of a whore", "spastic", "sweet Jesus", "twat", "wanker"
			)
		);
    	
    	Long authorId = (Long) request.getSession().getAttribute("authorId");
    	
    	if( authorId == null || authorId < 1L ) {
    		return ResponseEntity.badRequest().body("Login First");
    	}
    	
        if ( message == null || message.isEmpty() ) {
            return ResponseEntity.badRequest().body("Comment message cannot be empty.");
        }
        
        if( message.isEmpty() == false ) {
        	curse = curseWords.stream().anyMatch( word -> message.toLowerCase().contains(word.toLowerCase()));
        
        	if( curse ) {
        		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Comment message cannot be empty.");
        	}
        	
        }

        String sql = """
        			INSERT INTO PostComment(articleId, authorId, comment, createdat, commentType)
        			VALUES(? , ? , ? , CURRENT_TIMESTAMP, 'comment' )
        		""";
        jdbcTemplate.update(sql, articleId, authorId, message);
        
        
        sql = """
        		UPDATE Post
        		SET commentscount = commentscount + 1
        		WHERE articleId = ? 
        	""";
        jdbcTemplate.update(sql, articleId);
        
        // Process the comment (e.g., save to database)
        System.out.print("Comment Is " + message);

        // Return success response
        return ResponseEntity.ok().body(Map.of("message","Comment Posted Successfully"));
    }
    
    @GetMapping("/blogger/{id}")
    public String getBlogger(HttpServletRequest request, Model model, 
            @PathVariable Long id) {


        // Fetch userId from session
        Long userId = id;
        
        if (userId == null) {
            System.out.println("User not logged in. Redirecting to login.");
            model.addAttribute("error", "You need to log in to view your profile.");
            return "redirect:/";
        }
        
        if( userId != null && userId > 0 ) {
        	
        	String imageSQL = "SELECT profilePicture AS image FROM Blogger WHERE authorId = ?";
        	imageSQL = jdbcTemplate.queryForObject( imageSQL, String.class, userId );
        	
        	if( imageSQL == null || imageSQL.equals("") ) {
        		model.addAttribute("personalImage",null);
        	}else {
        		model.addAttribute("personalImage", bloggerRetrieveDirectory + imageSQL);
        	}
        	
        	
        }else {
        	model.addAttribute("personalImage",null);
        }
        

        String sql = "SELECT authorId,name,username,profilePicture AS image,createdat, bio FROM Blogger WHERE authorId = ?";
        List<Map<String, Object>> bloggers = jdbcTemplate.queryForList(sql, userId);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");
        String title = "Krish";
        
        for (Map<String, Object> current : bloggers) {
            Timestamp createdat = (Timestamp) current.get("createdat");
            title = current.get("name").toString();
            
//            System.out.println("Blogger Is " + blogger );
            Long author = (current.get("authorid") != null) ? (Long) current.get("authorId") : null;

            try {
                
                sql = """
	            		SELECT COUNT(*) AS followings FROM Connection WHERE followerId = 
	            		""" + author;
	            current.put("followings", jdbcTemplate.queryForObject(sql, Long.class)	 );

	            sql = """
	            		SELECT COUNT(*) AS followers FROM Connection WHERE followingId = 
	            		""" + author;
	            current.put("followers", jdbcTemplate.queryForObject(sql, Long.class) );
	
	            Long likes = 0L;
            	
	            sql = "SELECT articleId FROM Post WHERE primaryAuthor = ?";
	            List<Long> articles = jdbcTemplate.queryForList(sql, Long.class, author);
	            
	            for( Long articleId : articles ) {
	            
	            	sql = """
		            		SELECT COUNT(*) AS likes FROM PostInteraction WHERE articleId = 
		            	""" + articleId + " AND reactionType = 'like' ";
		            likes += jdbcTemplate.queryForObject(sql, Long.class);
	            
	            }
		            
	            current.put("likes", likes);
	            
	            sql = "SELECT articleId FROM Post WHERE primaryAuthor = " + author;
            	List<Long> ids = jdbcTemplate.queryForList(sql, Long.class);
            	
            	Long comments = 0L;
            	
            	for( Long articleId : ids ) {
            		
            		sql = """
	            		SELECT commentscount FROM Post WHERE articleId = 
	            	""" +  articleId;
            		
            		comments += jdbcTemplate.queryForObject(sql, Long.class);
            		
            	}
	            current.put("comments", comments );
	            
                sql = "SELECT COUNT(*) FROM Connection WHERE followerId = " + id + " AND followingId = " + author ;
                Long count = jdbcTemplate.queryForObject(sql, Long.class);
                current.put("status", count != null && count > 0 ); 

	            sql = """
	            		SELECT COUNT(*) AS posts FROM Post WHERE primaryAuthor =
	            		""" + userId;
	            current.put("posts", jdbcTemplate.queryForObject(sql, Long.class) );
	            
	        }catch( Exception e ) {
            	e.printStackTrace();
            	System.out.print("\n\n5" + "\n\n");
            	current.put("posts", 0);
            	current.put("followings", 0);
            	current.put("followingsList", null);
            	current.put("followers", 0);
            	current.put("followersList", null);
            	current.put("likes", 0);
            	current.put("status", false);
            	current.put("comments", 0);
	        }                       
            
            if (createdat != null) {
            	current.put("createdat", sdf.format(createdat));
            }
            
            if( current.get("image") == null || current.get("image").equals("") ) {
            	current.put("image", null);
            }else {
            	current.put("image", bloggerRetrieveDirectory + current.get("image"));
            }
            
        }
        model.addAttribute("bloggers", bloggers);
        model.addAttribute("title", title);
        
        try {
            // Fetch all IDs the current author is following
            sql = """
                  SELECT followingId FROM Connection WHERE followerId = 
                  """ + userId + " GROUP BY followingId";
            List<Long> followingIds = jdbcTemplate.queryForList(sql, Long.class);

            List<Map<String, Object>> followingDetails = new ArrayList<>();
            
            for( Long flwId : followingIds ) {
            	 sql = """
                      SELECT authorId, name, username, bio, profilePicture AS image 
                      FROM Blogger 
                      WHERE authorId = """ + flwId;
            	 List<Map<String, Object>> temp = jdbcTemplate.queryForList(sql);
            	 if( temp.get(0).get("image") == null || temp.get(0).get("image").equals("") ) {
            		 temp.get(0).put("image", null);
                 }else {
                	 temp.get(0).put("image", bloggerRetrieveDirectory + temp.get(0).get("image"));
                 }
            	 followingDetails.add( temp.get(0) );
            }
            model.addAttribute("followingsList", followingDetails);
            
            // Fetch details of these bloggers
            if (followingIds.isEmpty()) {
            	model.addAttribute("followingsList", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("\n\nError fetching followings\n\n");
            model.addAttribute("followingsList", null);
        }

        // Step 2: Fetch all follower IDs
        try {
            // Fetch all IDs of users who follow the current author
            sql = """
                  SELECT followerId FROM Connection WHERE followingId = 
                  """ + userId + " GROUP BY followerId";
            List<Long> followerIds = jdbcTemplate.queryForList(sql, Long.class);

            // Fetch details of these bloggers
            List<Map<String, Object>> followerDetails = new ArrayList<>();
            
            for( Long folId : followerIds ) {
            	 sql = """
                      SELECT authorId, name, username, bio, profilePicture AS image 
                      FROM Blogger 
                      WHERE authorId = """ + folId;
            	 List<Map<String, Object>> temp = jdbcTemplate.queryForList(sql);
            	 if( temp.get(0).get("image") == null || temp.get(0).get("image").equals("") ) {
            		 temp.get(0).put("image", null);
                 }else {
                	 temp.get(0).put("image", bloggerRetrieveDirectory + temp.get(0).get("image"));
                 }
            	 followerDetails.add( temp.get(0) );

            }
            model.addAttribute("followersList", followerDetails);
            
            // Fetch details of these bloggers
            if (followerIds.isEmpty()) {
                model.addAttribute("followersList", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("\n\nError fetching followings\n\n");
            model.addAttribute("followersList", null);
        }
        
        if (this.userExist != "" && userExist != null ) {
            model.addAttribute("loggedInUser", userExist); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
        String categorySql = "SELECT name FROM Category";
        List<String> categories = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("topics", categories);

        String postSql = """
                SELECT p.articleid, 
                       p.primaryAuthor AS author, 
                       p.title, 
                       p.description, 
                       p.likes, 
                       p.dislikes, 
                       p.viewscount, 
                       p.commentscount AS comments, 
                       p.updatedat, 
                       p.postmedia AS media,
                    p.poststatus AS status,
                       u.name AS name, 
                       u.username AS username, 
                       u.bio AS bio, 
                       u.profilepicture AS image,
                       c.name AS category
                FROM Post p 
                JOIN Blogger u ON p.primaryAuthor = u.authorid 
                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
                JOIN Category c ON pca.categoryid = c.categoryid
                WHERE u.authorid = ? ORDER BY p.createdat DESC
            """;
        
        	Long authorId = userId;

            List<Map<String, Object>> posts = jdbcTemplate.query(postSql, (rs, rowNum) -> {
                Map<String, Object> post = new HashMap<>();
                Long articleId = (Long) rs.getLong("articleid");
                
                List<HashMap<String,Object>> comment = new ArrayList<>();
                String commentsSQL = "SELECT authorId, comment, createdAt FROM PostComment WHERE articleId = ?";
                List<Map<String, Object>> commentTemp = jdbcTemplate.queryForList(commentsSQL, articleId);
                
                
                for( Map<String, Object> temporary : commentTemp ) {
                	
                	HashMap<String, Object> isItComment = new HashMap<>();

                	String personalSQL = "SELECT name, username, profilePicture AS image FROM Blogger WHERE authorId = ?";
                	List<Map<String, Object>> person = jdbcTemplate.queryForList(personalSQL, temporary.get("authorId"));
                    
                	isItComment.put("name", person.get(0).get("name"));
                	isItComment.put("authorId", temporary.get("authorId"));
                	isItComment.put("username", person.get(0).get("username"));
                	isItComment.put("comment", temporary.get("comment"));
                	
                	Object createdAtValue = temporary.get("createdat");
                	if (createdAtValue != null && createdAtValue instanceof String) {
                	    try {
                	        // Parse the string to a Date object
                	        String createdAtString = (String) createdAtValue;
                	        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to match input
                	        Date parsedDate = inputFormat.parse(createdAtString);

                	        // Format the parsed Date to the desired format
                	        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                	        String formattedDate = outputFormat.format(parsedDate);

                	        isItComment.put("createdat", formattedDate);
                	    } catch (Exception e) {
                	        System.err.println("Error parsing date: " + e.getMessage());
                	        isItComment.put("createdat", null);
                	    }
                	} else {
                	    isItComment.put("createdat", null); // Default value if null or not a String
                	}

                	
                	if( person.get(0).get("image") == null || person.get(0).get("image").equals("") ) {
                		isItComment.put("image", null);
                	}else {
                		isItComment.put("image", bloggerRetrieveDirectory + person.get(0).get("image") );
                	}
                	
                	comment.add( isItComment );
                	
                }
                
                post.put("postComments", comment);
                
                
                post.put("articleid", articleId);
                post.put("author", rs.getLong("author"));
                post.put("title", rs.getString("title"));
                post.put("disable",false);
                post.put("description", rs.getString("description"));
                post.put("likes", rs.getInt("likes"));
                post.put("dislikes", rs.getInt("dislikes"));
                post.put("viewscount", rs.getInt("viewscount"));
                post.put("comments", rs.getInt("comments"));
                Timestamp timestamp = rs.getTimestamp("updatedat");
                if (timestamp != null) {
                    SimpleDateFormat ssdf = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
                    String formattedDate = ssdf.format(timestamp);
                    post.put("updatedat", formattedDate);
                } else {
                    post.put("updatedat", null);
                }
                post.put("name", rs.getString("name"));
                post.put("username", rs.getString("username"));
                post.put("bio", rs.getString("bio"));
                post.put("status", rs.getString("status"));
                post.put("category", rs.getString("category"));
                if( rs.getString("media") == null || rs.getString("media").equals("") ) {
	post.put("media", null);
}else {
    post.put("media", postRetrieveDirectory + rs.getString("media"));
}
                if( rs.getString("image") == null || rs.getString("image").equals("") ) {
	post.put("image", null);
}else {
    post.put("image", bloggerRetrieveDirectory + rs.getString("image"));
}

                // Separate query to get keywords for the current article
                String keywordQuery = """
                		SELECT name FROM Keyword k 
                		JOIN KeywordAssignment ka 
                		ON k.keywordid = ka.keywordid 
                		WHERE ka.articleid = ?
                	""";
            	List<String> keywords = jdbcTemplate.queryForList(keywordQuery, String.class, (Long) articleId);
            	post.put("keywords", keywords);

            	if( authorId != null && authorId > 0 ) {

            		String isReacted = "SELECT COUNT(*) AS liked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'like'";
            		Long isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
            	
	            	post.put("isLiked", isReact == 0 ? false : true);
	            	
	            	isReacted = "SELECT COUNT(*) AS disliked FROM PostInteraction WHERE articleId = ? AND authorId = ? AND reactiontype = 'dislike'";
	            	isReact= jdbcTemplate.queryForObject(isReacted, Long.class, articleId, authorId );
	            	
	            	post.put("isDisliked", isReact == 0 ? false : true );
            	
            	}else {
            		post.put("isLiked", false );            		
            		post.put("isDisliked", false );
            	}
            	
                return post;
            }, authorId);

        model.addAttribute("posts", posts);

        System.out.print(posts);

    	List<String> colors = new ArrayList<>(
    			List.of(
                    "red", "green", "blue", "yellow", "cyan", "magenta", "orange", "purple",
                    "lime", "teal", "indigo", "gold", "deeppink", "mediumseagreen", "darkorange",
                    "dodgerblue", "crimson", "orangered", "mediumvioletred", "chartreuse",
                    "turquoise", "darkturquoise", "springgreen", "mediumspringgreen", "lightseagreen",
                    "steelblue", "mediumblue", "mediumorchid", "mediumturquoise", "darkcyan",
                    "royalblue", "forestgreen", "seagreen", "cadetblue", "tomato", "sienna",
                    "hotpink", "salmon", "darksalmon", "lightsalmon", "cornflowerblue", "slateblue",
                    "mediumslateblue", "skyblue", "mediumaquamarine", "palevioletred",
                    "darkgoldenrod", "olive", "darkkhaki", "darkseagreen", "firebrick", "peru"
                )
    		);
    	
    	model.addAttribute("colors", colors);
    	model.addAttribute("followers", null);
    	model.addAttribute("followings", null);
        
        // Add logged-in username if available
        String loggedInUser = (String) request.getSession().getAttribute("loggedInUser");
        model.addAttribute("loggedInUser", loggedInUser != null ? loggedInUser : "Guest");
        
        return "author"; // Return the author management view
    }

}