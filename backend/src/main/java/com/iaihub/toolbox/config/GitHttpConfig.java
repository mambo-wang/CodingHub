package com.iaihub.toolbox.config;

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.transport.resolver.FileResolver;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 内置 git 服务器（Smart HTTP 协议）。
 *
 * <p>为每个插件维护一个 bare 仓库，通过 {@code /git/{name}.git} 提供
 * {@code git clone} / {@code git fetch} 能力，供 CodeBuddy 的 URL 市场
 * （source 对象 {@code {source:"url", url:".../git/<name>.git"}}）安装插件。</p>
 *
 * <p>仓库根目录复用上传目录：{@code <app.upload.base-dir>/git-repos/}。仅开放匿名只读，
 * 不开放推送。</p>
 */
@Configuration
public class GitHttpConfig {

    @Bean
    public ServletRegistrationBean<Servlet> gitServletRegistrationBean(UploadConfig uploadConfig) {
        File root = new File(uploadConfig.getBaseDir(), "git-repos");
        if (!root.exists()) {
            root.mkdirs();
        }

        GitServlet gitServlet = new GitServlet();
        // FileResolver: 将 URL 路径映射到 root 下的仓库，exportAll=true 仅允许读取
        FileResolver<HttpServletRequest> resolver =
                new FileResolver<>(root, true);
        gitServlet.setRepositoryResolver(resolver);

        ServletRegistrationBean<Servlet> bean = new ServletRegistrationBean<>(gitServlet);
        bean.addUrlMappings("/git/*");
        bean.setLoadOnStartup(1);
        return bean;
    }
}
