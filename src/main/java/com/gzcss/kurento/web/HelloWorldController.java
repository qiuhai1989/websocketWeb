package com.gzcss.kurento.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/hello")
public class HelloWorldController {

    private static Logger log = LoggerFactory.getLogger(HelloWorldController.class);

    @RequestMapping("hello.html")
    public ModelAndView sayHello(@RequestParam("name") String name){
        log.info("name="+name);
        ModelAndView model = new ModelAndView("hello");
        model.addObject("name",name);
        return model;
    }

}
