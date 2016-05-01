/*
cd src/uk/ac/bris/cs/databases/cwk3
cd ../../../../../../..

ANT ON UNIX - Note for Alex
export ANT_HOME=/Library/Ant
export PATH=${PATH}:${ANT_HOME}/bin
*/

package uk.ac.bris.cs.databases.cwk3;

import java.sql.*;
//import java.util.List;
import java.util.*;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import uk.ac.bris.cs.databases.api.APIProvider;
import uk.ac.bris.cs.databases.api.AdvancedForumSummaryView;
import uk.ac.bris.cs.databases.api.AdvancedForumView;
import uk.ac.bris.cs.databases.api.ForumSummaryView;
import uk.ac.bris.cs.databases.api.ForumView;
import uk.ac.bris.cs.databases.api.AdvancedPersonView;
import uk.ac.bris.cs.databases.api.PostView;
import uk.ac.bris.cs.databases.api.Result;
import uk.ac.bris.cs.databases.api.PersonView;
import uk.ac.bris.cs.databases.api.SimpleForumSummaryView;
import uk.ac.bris.cs.databases.api.SimplePostView; //ALEX JUST ADDED THIS - SHOULD WE NEED TO BE DOING THIS?
import uk.ac.bris.cs.databases.api.SimpleTopicView;
import uk.ac.bris.cs.databases.api.TopicView;
import uk.ac.bris.cs.databases.util.Params;
import uk.ac.bris.cs.databases.web.ApplicationContext;

/**
 *
 * @author csxdb
 */
public class API implements APIProvider {

    private final Connection c;

    public API(Connection c) {
        this.c = c;
    }

    private static final String DATABASE = "jdbc:sqlite:database/database.sqlite3";
    public static void main(String[] args){
      //SET UP FOR TESTS
         ApplicationContext c = ApplicationContext.getInstance();
         APIProvider api;
         Connection conn;
         try{
            conn = DriverManager.getConnection(DATABASE);
            conn.setAutoCommit(false);
            api = new API(conn);
            c.setApi(api);
         } catch (SQLException e) {
            throw new RuntimeException(e);
         }

      //TESTS
         int passed = 0;
         int failed = 0;
         
         //--getUsers
         if(api.test(api.getUsers(), "success")) passed++;
         else {api.p("Failed getUsers"); failed++; }
         
         //--getPersonView
         if(api.test(api.getPersonView("tb15269"), "success")) passed++;
         else {api.p("Failed getPersonView 1"); failed++; }
         
         if(api.test(api.getPersonView("tb1269"), "fatal")) passed++;
         else {api.p("Failed getPersonView 2"); failed++; }
         
         //--getSimpleForums
         if(api.test(api.getSimpleForums(), "success")) passed++;
         else {api.p("Failed getUsers"); failed++; }
         
         //--countPostsInTopic
         if(api.test(api.countPostsInTopic(1), "success")) passed++;
         else {api.p("Failed countPostsInTopic"); failed++; }
         
         //--getLikers
         //if(api.test(api.getLikers(1), "success")) passed++;
         //else {api.p("Failed countPostsInTopic"); failed++; }
         
         //--getSimpleTopic
         if(api.test(api.getSimpleTopic(1), "success")) passed++;
         else {api.p("Failed getSimpleTopic1"); failed++; }
         
         if(api.test(api.getSimpleTopic(100), "failure")) {api.p("Failed getSimpleTopic2"); failed++; }
         else passed++; 
         
         //--getLatestPost
         if(api.test(api.getLatestPost(1), "success")) passed++;
         else {api.p("Failed getLatestPost1"); failed++; }
         
         if(api.test(api.getLatestPost(100), "failure")) {api.p("Failed getLatestPost2"); failed++; }
         else passed++; 
         
         /*
         we should make database/unitTests.sqlite3 and load that instead of
         one that will keep changing as we play with the forum.
         
         getUsers()
         getPersonView(String username)
         getSimpleForums()
         countPostsInTopic(long topicId)
         getLikers(long topicId)
         getSimpleTopic(long topicId)     
         //Level 2
         getLatestPost(long topicId)
         
         getForums()
         createForum(String title)
         createPost(long topicId, String username, String text)
         addNewPerson(String name, String username, String studentId)
         getForum(long id)
         getTopic(long topicId, int page)
         likeTopic(String username, long topicId, boolean like)
         favouriteTopic(String username, long topicId, boolean fav)
         //LEVEL 3
         createTopic(long forumId, String username, String title, String text)
         getAdvancedForums()
         getAdvancedPersonView(String username)
         getAdvancedForum(long id)
         likePost(String username, long topicId, int post, boolean like)
         */
         
         api.p("Passed " + passed + " tests. Failed " + failed);
    }
    
    @Override
    public void p(String s){
       System.out.println(s);
    }
    
    @Override
    public boolean test(Result r, String expectedResult){
       try{
         switch(expectedResult){
            case "success":
               if(r.isSuccess()) return true;
               return false;
            case "failure":
               if(!r.isFatal()) return true; //isFatal returns false if is failiure, exception if success, true if fatal
               return false;
            case "fatal":
               if(r.isFatal()) return true;
               return false;
            default:
               System.err.println("Test configured incorrectly");
               return false;
         }
       }catch(RuntimeException e){
          return false;
       }
    }
    
   //Test with: http://localhost:8000/people
   @Override
   public Result<Map<String, String>> getUsers() {
   	try(
   		PreparedStatement s = c.prepareStatement(
               "SELECT username, name FROM Person;"
   		);
         ){
            Map<String, String> map = new HashMap<>();
            ResultSet r = s.executeQuery();
            while(r.next()){
               map.put(r.getString("username"), r.getString("name"));
            }
            return Result.success(map);
   	}catch (SQLException ex) {
         printError("Error in getUsers: " + ex.getMessage());
   	}
   	return Result.fatal("Fatal getUsers");
   }

   //Test with: http://localhost:8000/person/tb15269
   @Override
   public Result<PersonView> getPersonView(String username) {
      Params.cannotBeEmpty(username);
      Params.cannotBeNull(username);
      try(
            PreparedStatement s = c.prepareStatement(
               "SELECT name, username, stuId FROM Person " + "WHERE username = ?"
            );
         ){
         s.setString(1, username);
         ResultSet r = s.executeQuery();
         PersonView pv = new PersonView(r.getString("name"),
                                        r.getString("username"),
                                        r.getString("stuId"));
         return Result.success(pv);
      }catch (SQLException ex) {
         printError("Error in getPersonView: " + ex.getMessage());
      }
      return Result.fatal("Fatal getPersonView");
   }

    //Test with: http://localhost:8000/forums0
    @Override
    public Result<List<SimpleForumSummaryView>> getSimpleForums() {
      try(
            PreparedStatement s = c.prepareStatement(
               "SELECT id, title FROM Forum;"
            );
         ){
         ResultSet r = s.executeQuery();
         List<SimpleForumSummaryView> simpleForumsList = new ArrayList<>();
         while (r.next()) {
            SimpleForumSummaryView sfsv = new SimpleForumSummaryView(r.getLong("id"),
                                                                     r.getString("title"));
            simpleForumsList.add(sfsv);
         }
         return Result.success(simpleForumsList);
      }catch (SQLException ex) {
         printError("Error in getSimpleForums: " + ex.getMessage());
      }
      return Result.fatal("Fatal getSimpleForums");
    }

    //TEST WITH
    @Override
    public Result<Integer> countPostsInTopic(long topicId) {
       try{
         PreparedStatement s = c.prepareStatement(
               "SELECT COUNT(*) AS numposts FROM POST WHERE topicid = ?"
            );
         s.setLong(1, topicId);
         ResultSet r = s.executeQuery();
         return Result.success(r.getInt("numposts"));
       }catch(SQLException ex){
         printError("Error in getSimpleTopic: " + ex.getMessage());
       }
       return Result.fatal("Fatal getPostsInTopic");
    }

    @Override
    public Result<List<PersonView>> getLikers(long topicId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /* Test with: http://localhost:8000/topic0/1
       or
       Test with: http://localhost:8000/topic0/2

       SQL query:
       SELECT t.id as topicid, t.title, p.id as postid, per.username, p.text, p.date FROM Topic AS t JOIN Post AS p ON t.id = p.topicid JOIN Person AS per ON p.authorid = per.id WHERE t.id = 1;
    */

    @Override
    public Result<SimpleTopicView> getSimpleTopic(long topicId) {
      try(
            PreparedStatement s = c.prepareStatement(
               "SELECT t.id as topicid, t.title, p.id as postid, per.username, p.text, p.date FROM Topic AS t " +
               "JOIN Post AS p ON t.id = p.topicid " +
               "JOIN Person AS per ON p.authorid = per.id " +
               "WHERE t.id = ?"
            );
         ){
         s.setLong(1, topicId);

         ResultSet r = s.executeQuery();

         String topicTitle = r.getString("title");

         //Collect post and add to list
         List<SimplePostView> simplePostsList = new ArrayList<>();
         while (r.next()) {
            SimplePostView spv = new SimplePostView(r.getInt("postid"),
                                                    r.getString("username"),
                                                    r.getString("text"),
                                                    r.getInt("date"));
            simplePostsList.add(spv);
         }

         //Create simpleTopicView pass list of posts
         SimpleTopicView stv = new SimpleTopicView(topicId,
                                                   topicTitle,
                                                   simplePostsList);

         return Result.success(stv);

      }catch (SQLException ex) {
         printError("Error in getSimpleTopic: " + ex.getMessage());
      }
      return Result.fatal("Fatal getSimpleTopic");
    }

    //testwith: 
    @Override
    public Result<PostView> getLatestPost(long topicId) {
       try{
         PreparedStatement s = c.prepareStatement(
               "SELECT forum.id as forumid,         post.topicid as topicid, " + 
                      "post.id as postNumber,       person.name as authorname, " +
                      "person.username as username, post.text as text, " + 
                      "post.date as postedAt,       likes.numLikes as numberOfLikes " +
                      "FROM Post " +
               "JOIN Person ON Post.authorid = Person.id " +
               "JOIN Topic ON Post.topicid = Topic.id " +
               "JOIN Forum ON Topic.forumid = Forum.id " +
               "JOIN (SELECT postid, count(*) as numLikes FROM Post_Likers GROUP BY postid) as Likes ON Likes.postid = Post.id " +
               "WHERE topicid = ? " +
               "ORDER BY postNumber DESC LIMIT 1;"
            );
         s.setLong(1, topicId);
         ResultSet r = s.executeQuery();
         
         //PostView(long forumId, long topicId, int postNumber, String authorName, String authorUserName, String text, int postedAt, int likes)
         PostView pv = new PostView(r.getLong("forumid"),
                                    r.getLong("topicid"),
                                    r.getInt("postNumber"),
                                    r.getString("authorname"),
                                    r.getString("username"),
                                    r.getString("text"),
                                    r.getInt("postedAt"),
                                    r.getInt("numberOfLikes"));
         return Result.success(pv);
       }catch(SQLException ex){
         printError("Error in getSimpleTopic: " + ex.getMessage());
       }
       return Result.fatal("Fatal getSimpleTopic");
    }

    @Override
    public Result<List<ForumSummaryView>> getForums() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result createForum(String title) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result createPost(long topicId, String username, String text) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result addNewPerson(String name, String username, String studentId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<ForumView> getForum(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<TopicView> getTopic(long topicId, int page) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result likeTopic(String username, long topicId, boolean like) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result favouriteTopic(String username, long topicId, boolean fav) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result createTopic(long forumId, String username, String title, String text) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<List<AdvancedForumSummaryView>> getAdvancedForums() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<AdvancedPersonView> getAdvancedPersonView(String username) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result<AdvancedForumView> getAdvancedForum(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Result likePost(String username, long topicId, int post, boolean like) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void printError(String s){
       System.err.println(s);
    }

    private void printDebug(String s){
       System.out.println("\\x1b[32m" + s + "\\x1b[0m");
    }

   }
