package com.example.tech;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;
import java.util.Map;
import java.security.Principal;
import java.util.HashMap;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Controller
public class JdbcController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static Integer start = 1;

    @GetMapping("/") // Maps to the root URL (http://localhost:8080/)
    public String home(Model model, Principal principal) {
        
    	if (principal != null) {
            model.addAttribute("loggedInUser", principal.getName()); // Add the logged-in username
        } else {
            model.addAttribute("loggedInUser", null); // No user logged in
        }
        
    	String categorySql = "SELECT name FROM Category";
        List<String> categories = jdbcTemplate.queryForList(categorySql, String.class);
        model.addAttribute("topics", categories);

        // Fetch post details along with author details and category name
        String postSql = "SELECT p.articleid, p.title, p.description,p.likes, p.dislikes, p.viewscount, p.commentscount, p.updatedat ,u.name AS name, u.username AS username, u.bio AS bio, c.name AS category " +
                         "FROM Post p " +
                         "JOIN Blogger u ON p.primaryAuthor = u.authorid " +
                         "JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid " +
                         "JOIN Category c ON pca.categoryid = c.categoryid " +
                         "LIMIT 5";
        List<Map<String, Object>> posts = jdbcTemplate.queryForList(postSql);
        model.addAttribute("posts", posts);
        
        return "home"; // Home page
    }
    
    // Get all bloggers
    @GetMapping("/bloggers")
    public String getAllBloggers(Model model) {
        String sql = "SELECT * FROM Blogger";
        List<Map<String, Object>> bloggers = jdbcTemplate.queryForList(sql);
        model.addAttribute("bloggers", bloggers);
        return "bloggers";
    }
    
    // Show all post
    @GetMapping("/posts")
    public String showpost(Model model) {
    	String sql = "SELECT p.articleid, p.title, p.description,p.likes, p.dislikes, p.viewscount, p.commentscount, p.updatedat ,u.name AS name, u.username AS username, u.bio AS bio, c.name AS category " +
                "FROM Post p " +
                "JOIN Blogger u ON p.primaryAuthor = u.authorid " +
                "JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid " +
                "JOIN Category c ON pca.categoryid = c.categoryid ";
        List<Map<String, Object>> posts = jdbcTemplate.queryForList(sql);
        model.addAttribute("posts", posts);
        return "show"; // Assuming there's a Thymeleaf template named "show"
    }
    
    @PostMapping("/review")
    public String reviewPost(Model model, String title, String category, String description) {
        Map<String, Object> postData = new HashMap<>();
        postData.put("title", title);
        postData.put("category", category);
        description = description.replaceAll("<[^>]*>", "").trim();
        postData.put("description", description);

        // Add the Map to the model
        model.addAttribute("post", postData);
        
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
        String sql = "SELECT * FROM Post WHERE articleid = ?";
        List<Map<String, Object>> post = jdbcTemplate.queryForList(sql, id);
        model.addAttribute("post", post);
        return "viewPost"; // Assuming there's a Thymeleaf template named "viewPost"
    }

    
    @GetMapping("/filter/{category}")
    public String filterpost(Model model, @PathVariable String category) {
    	
    	String sql = "SELECT categoryId FROM Category WHERE LOWER(name) LIKE LOWER(?)";
        List<Integer> categories = jdbcTemplate.queryForList(sql, Integer.class, "%" + category + "%");

        if (categories.isEmpty()) {
            model.addAttribute("error", "No posts found in this category.");
            return "filter"; // Return with error message if no category found
        }

        int categoryId = categories.get(0);

        sql = """
                SELECT p.articleid, p.title, p.description, p.likes, p.dislikes, p.viewscount, 
                       p.commentscount, p.updatedat, u.name AS name, u.username AS username, 
                       u.bio AS bio, c.name AS category 
                FROM Post p 
                JOIN Blogger u ON p.primaryAuthor = u.authorid 
                JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid 
                JOIN Category c ON pca.categoryid = c.categoryid 
                WHERE c.categoryid = ?
              """;

        List<Map<String, Object>> posts = jdbcTemplate.queryForList(sql, categoryId);

        // Print SQL query and data for debugging (optional for dev use)
        System.out.println("Executed Query: " + sql + " | Category: " + category + " | Posts: " + posts);

        if (posts.isEmpty()) {
            model.addAttribute("error", "No posts found for this category.");
        } else {
            model.addAttribute("posts", posts);
        }

        model.addAttribute("topic", category);
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
    	
        return "createPost"; // Create post page
    }
    

    @GetMapping("/profile")
    public String getProfile(Model model) {
    	
    	String sql = "SELECT * FROM Blogger";
    	List<Map<String,Object>> user = jdbcTemplate.queryForList(sql);
        
        model.addAttribute("user", user.get(0));
    	return "profilemanagement";
    }
    
    @PostMapping("/search")
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

    		// Return the "filter" view
    		return "filter";
    }
    
    // Create a new post
    @PostMapping("/doPost")
    public String makePost(Model model, String title, String categoryName, String description) {
        // Clean up the description by removing HTML tags and trimming
        description = description.replaceAll("<[^>]*>", "").trim();
        int primaryAuthorId = 1;
        
        String getMaxIdSql = "SELECT COALESCE(MAX(articleid), 0) + 1 FROM Post ";
        Integer newArticleId = jdbcTemplate.queryForObject(getMaxIdSql, Integer.class);

        String insertPostSql = """
            INSERT INTO Post (articleid, title, description, likes, dislikes, commentscount, primaryauthor, viewscount, postmedia, publishedat, createdat, updatedat) 
            VALUES (?, ?, ?, 0, 0, 0, ?, 0, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;

        System.out.println("\nBefore Executing Post Insert Query");

        int rowsAffected = jdbcTemplate.update(
            insertPostSql,
            newArticleId,  // Manually generated article ID
            title,
            description,
            primaryAuthorId
        );
        
        getMaxIdSql = "SELECT COALESCE(MAX(postcategoryassignmentid), 0) + 1 FROM postcategoryassignment ";
        Integer newPAId = jdbcTemplate.queryForObject(getMaxIdSql, Integer.class);

        String insertAssignmentSql = """
                INSERT INTO PostCategoryAssignment (postcategoryassignmentid, articleid, categoryid, assignedby , createdat) 
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
            jdbcTemplate.update(insertAssignmentSql, newPAId, newArticleId, 1, primaryAuthorId); // Use newArticleId and categoryId here
            System.out.println("\nAfter Executing PCA Insert Query");

        System.out.println("\nAfter Executing Post Insert Query. Rows affected: " + rowsAffected);
        
        return "redirect:/data";
    }
    
    @GetMapping("/load-more-post")
    @ResponseBody
    public Map<String, Object> loadMorepost(@RequestParam("page") int page) {
        int pageSize = 5; // Number of post per page
        int offset = (page - 1) * pageSize;

        // SQL query to fetch post based on the page and pageSize
        String sql = "SELECT p.articleid, p.title, p.description,p.likes, p.dislikes, p.viewscount, p.commentscount, p.updatedat ,u.name AS name, u.username AS username, u.bio AS bio, c.name AS category " +
                "FROM Post p " +
                "JOIN Blogger u ON p.primaryAuthor = u.authorid " +
                "JOIN PostCategoryAssignment pca ON p.articleid = pca.articleid " +
                "JOIN Category c ON pca.categoryid = c.categoryid " +
                "ORDER BY articleid LIMIT ? OFFSET ?";
        List<Map<String, Object>> post = jdbcTemplate.queryForList(sql, pageSize, offset);

        // Check if there are more post to load
        boolean hasMore = post.size() == pageSize;

        // Prepare the response map
        Map<String, Object> response = new HashMap<>();
        response.put("post", post);
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
            			(1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Coding is a category in which can explore the posts and articles regarding coding, coding memes and code of variety of programming languages and code', 'coding.jpg', 'Coding'),
            			(2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Technology is a category in which can explore the posts and articles regarding technology and current trends of technologies', 'technology.jpg', 'Technology'),
						(3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Robotics is a category in which can explore the posts and articles regarding robotics and the advance research related works for automation and revolutionary robotics field', 'robotics.jpg', 'Robotics'),
						(4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'News is a category in which can explore the posts and articles regarding the latest news regarding any information on category also it makes you more updated on industry', 'news.jpg', 'News');
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
    public String register(Model model, String name, String username, String email, String password, String confirmPassword, String bio) {

        // Check if the user already exists (by email or username)
        String checkUserSql = "SELECT COUNT(*) FROM Blogger WHERE email = ? OR username = ?";
        Integer existingUserCount = jdbcTemplate.queryForObject(checkUserSql, Integer.class, email, username);

        if (existingUserCount != null && existingUserCount > 0) {
            model.addAttribute("error", "Username or Email already exists!");
            return "register"; // Return to registration page with error message
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
            return "redirect:/login?success=true"; // Redirect to login with success flag
        } else {
            model.addAttribute("error", "Registration failed. Please try again.");
            return "register";
        }
    }

    
}