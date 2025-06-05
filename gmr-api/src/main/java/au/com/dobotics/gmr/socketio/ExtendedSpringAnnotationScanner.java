package au.com.dobotics.gmr.socketio;

import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import org.springframework.beans.BeansException;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collection;

public class ExtendedSpringAnnotationScanner extends SpringAnnotationScanner {

    private SocketIOServer socketIOServer;

    public ExtendedSpringAnnotationScanner(SocketIOServer socketIOServer) {
        super(socketIOServer);
        this.socketIOServer = socketIOServer;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Field field = ReflectionUtils.findField(this.getClass(), "originalBeanClass", Class.class);
        field.setAccessible(true);
        Class originalBeanClass = (Class) ReflectionUtils.getField(field, this);
        if (originalBeanClass != null) {
            super.postProcessAfterInitialization(bean, beanName);
//            Collection<SocketIONamespace> allNamespaces = this.socketIOServer.getAllNamespaces();
//            for (SocketIONamespace namespace : allNamespaces) {
//                namespace.addListeners(bean, bean.getClass());
//            }
        }
        return bean;
    }
}
