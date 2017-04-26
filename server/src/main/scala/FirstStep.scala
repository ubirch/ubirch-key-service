/**
  * Created by derMicha on 17/03/17.
  */
object FirstStep extends App {

  import org.anormcypher._

  // Setup the Rest Client
  implicit val connection = Neo4jREST(username = "apiuser", password = "apiuser")

  // create some test nodes
  Cypher("""create (a:Test {name:"AnormCypher2"}), (b:Test {name:"Test1"})""").execute()

  // a simple query
  val req = Cypher("match (c:Test) return c.name")

  // get a stream of results back
  val stream = req()

  // get the results and put them into a list
  val l = stream.map(row => {
    row[String]("c.name")
  }).toList

  println(l)
}
