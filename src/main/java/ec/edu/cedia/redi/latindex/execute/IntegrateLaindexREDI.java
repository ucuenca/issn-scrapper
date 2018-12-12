/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.cedia.redi.latindex.execute;

import ec.edu.cedia.redi.latindex.repository.Redi;
import ec.edu.cedia.redi.latindex.repository.RediRepository;
import org.openrdf.repository.RepositoryException;

/**
 *
 * @author cedia
 */
public class IntegrateLaindexREDI {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws RepositoryException, Exception {
        try (RediRepository r = RediRepository.getInstance()) {
            Redi redi = new Redi(r);
            //redi.updateOtherIndexes();
            //redi.updateLatindex();
            redi.updateLatindexImg();
        }
    }

}
