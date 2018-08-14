/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.cedia.redi.graphdb.centralgraph;

import java.io.UnsupportedEncodingException;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.RepositoryException;

/**
 *
 * @author cedia
 */
public class generateGraph {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws UnsupportedEncodingException, RepositoryException, MalformedQueryException, UpdateExecutionException {
        // TODO code application logic here

        graphdbRepository g = new graphdbRepository();
        //---OK
//        g.replaceSameAsSubject(graphdbRepository.AUTHORS_SA, graphdbRepository.AUTHORS_SA + "F", graphdbRepository.AUTHORS_SA2);
//        g.removeDuplicatedSameAs(graphdbRepository.COAUTHORS_SA, graphdbRepository.COAUTHORS_SA + "F");
//        g.removeDuplicatedSameAs(graphdbRepository.PUBLICATIONS_SA, graphdbRepository.PUBLICATIONS_SA + "F");
//        g.harvestRawData(graphdbRepository.AUTHORS_SA + "F", graphdbRepository.BASE_CONTEXT + "rediRaw", 3);
//        g.copyGraph(graphdbRepository.AUTHORS_SA + "F", graphdbRepository.BASE_CONTEXT + "sa");
//        g.copyGraph(graphdbRepository.COAUTHORS_SA + "F", graphdbRepository.BASE_CONTEXT + "sa");
//        g.copyGraph(graphdbRepository.PUBLICATIONS_SA + "F", graphdbRepository.BASE_CONTEXT + "sa");

//        g.replaceSameAsSubject(graphdbRepository.BASE_CONTEXT + "rediRaw",
//                graphdbRepository.BASE_CONTEXT + "rediRawS", graphdbRepository.BASE_CONTEXT + "sa");
//        g.replaceSameAsObjectExcept(graphdbRepository.BASE_CONTEXT + "rediRawS",
//                graphdbRepository.BASE_CONTEXT + "redi", graphdbRepository.BASE_CONTEXT + "sa",
//                "http://xmlns.com/foaf/0.1/holdsAccount");
//        g.copyGraph(graphdbRepository.BASE_CONTEXT + "sa", graphdbRepository.BASE_CONTEXT + "redi");
        g.deleteGraph(graphdbRepository.BASE_CONTEXT + "rediRaw");
        g.deleteGraph(graphdbRepository.BASE_CONTEXT + "rediRawS");





//        g.copyGraph(graphdbRepository.AUTHORS_SA, graphdbRepository.BASE_CONTEXT + "sa");
//        g.copyGraph(graphdbRepository.COAUTHORS_SA, graphdbRepository.BASE_CONTEXT + "sa");
//        g.removeDuplicatedPrior(graphdbRepository.AUTHORS_SA, graphdbRepository.COAUTHORS_SA, 
//                graphdbRepository.BASE_CONTEXT + "sa", graphdbRepository.BASE_CONTEXT + "saF");
    }

}
