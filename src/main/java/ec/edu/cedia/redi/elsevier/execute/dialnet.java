/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.cedia.redi.elsevier.execute;

import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author cedia
 */
public class dialnet {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // TODO code application logic here
        Document get = null;
        get = Jsoup.connect("https://dialnet.unirioja.es/buscar/documentos?querysDismax.DOCUMENTAL_TODO=Jorge+Folino").get();
        Element first = get.select("#listadoDeArticulos").first();
        for (Element a : first.children()) {
            String id = a.select(".descripcion .titulo .titulo a").attr("href").split("=")[1];
            hvDoc(id);
        }
    }

    public static void hvDoc(String id) throws IOException, InterruptedException {
        Thread.sleep(10000);
        Document get = Jsoup.connect("https://dialnet.unirioja.es/servlet/articulo?codigo="+id).get();
        String text = get.select("#articulo h2 .titulo").text();
        System.out.println(text);
    }

}
