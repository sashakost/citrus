/*
 * Copyright 2006-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.admin.controller;

import com.consol.citrus.admin.service.ConfigurationService;
import com.consol.citrus.admin.util.FileHelper;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;

/**
 * @author Christoph Deppisch
 */
@Controller
@RequestMapping("/project")
public class ProjectController {
    
    @Autowired
    private ConfigurationService configService;
    
    @Autowired
    private FileHelper fileHelper;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ModelAndView searchProjectHome(@RequestParam("dir") String dir) {
        String directory = FilenameUtils.separatorsToSystem(fileHelper.decodeDirectoryUrl(dir, configService.getRootDirectory()));
        String[] folders = fileHelper.getFolders(new File(directory));

        ModelAndView view = new ModelAndView("FileTree");
        view.addObject("folders", folders);
        view.addObject("baseDir", FilenameUtils.separatorsToUnix(directory));

        return view;
    }
    
    @RequestMapping(params = {"projecthome"}, method = RequestMethod.GET)
    public String setProjectHome(@RequestParam("projecthome") String projecthome) {
        if (!configService.isProjectHome(projecthome)) {
            throw new IllegalArgumentException("Invalid project home - not a proper Citrus project");
        }
        
        configService.setProjectHome(projecthome);
        return "redirect:/";
    }
}
