package com.univalle.parkingmanagementservice.usuario.service.serviceImpl;
import com.univalle.parkingmanagementservice.auth.entities.EstadoUsuario;
import com.univalle.parkingmanagementservice.auth.entities.Rol;
import com.univalle.parkingmanagementservice.auth.entities.Usuario;
import com.univalle.parkingmanagementservice.auth.repositories.EstadoUsuarioRepository;
import com.univalle.parkingmanagementservice.auth.repositories.RolRepository;
import com.univalle.parkingmanagementservice.auth.repositories.UsuarioRepository;
import com.univalle.parkingmanagementservice.usuario.dto.CrearUsuarioRequest;
import com.univalle.parkingmanagementservice.usuario.dto.EditarUsuarioRequest;
import com.univalle.parkingmanagementservice.usuario.dto.UsuarioListItemResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private EstadoUsuarioRepository estadoUsuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private CrearUsuarioRequest request;
    private Rol rolAuxiliar;
    private Rol rolAdministrador;
    private EstadoUsuario estadoActivo;
    private Usuario usuarioExistente;

    @BeforeEach
    void setUp() {
        request = new CrearUsuarioRequest(
                "Juan Pérez",
                "jperez",
                "Admin123*",
                "Admin123*",
                "AUXILIAR"
        );

        rolAuxiliar = new Rol();
        rolAuxiliar.setId(2L);
        rolAuxiliar.setNombre("AUXILIAR");

        rolAdministrador = new Rol();
        rolAdministrador.setId(1L);
        rolAdministrador.setNombre("ADMINISTRADOR");

        estadoActivo = new EstadoUsuario();
        estadoActivo.setId(1L);
        estadoActivo.setNombre("ACTIVO");

        usuarioExistente = new Usuario();
        usuarioExistente.setId(10L);
        usuarioExistente.setNombreCompleto("Juan Pérez");
        usuarioExistente.setNombreUsuario("jperez");
        usuarioExistente.setContrasenaHash("HASH_OLD");
        usuarioExistente.setRol(rolAuxiliar);
        usuarioExistente.setFechaCreacion(OffsetDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }


    @Test
    void deberiaListarUsuariosOrdenadosPorNombreYMapearRespuesta() {
        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMINISTRADOR");

        Rol rolUser = new Rol();
        rolUser.setNombre("AUXILIAR");

        Usuario usuario1 = new Usuario();
        usuario1.setId(1L);
        usuario1.setNombreCompleto("Carlos Pérez");
        usuario1.setNombreUsuario("cperez");
        usuario1.setRol(rolUser);

        Usuario usuario2 = new Usuario();
        usuario2.setId(2L);
        usuario2.setNombreCompleto("ana López");
        usuario2.setNombreUsuario("alopez");
        usuario2.setRol(rolAdmin);

        when(usuarioRepository.findAll()).thenReturn(List.of(usuario1, usuario2));

        List<UsuarioListItemResponse> resultado = usuarioService.listarUsuarios();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());

        // Validar orden case-insensitive: "ana López" debe ir antes que "Carlos Pérez"
        assertEquals(2L, resultado.get(0).id());
        assertEquals("ana López", resultado.get(0).nombreCompleto());
        assertEquals("alopez", resultado.get(0).nombreUsuario());
        assertEquals("ADMINISTRADOR", resultado.get(0).rol());

        assertEquals(1L, resultado.get(1).id());
        assertEquals("Carlos Pérez", resultado.get(1).nombreCompleto());
        assertEquals("cperez", resultado.get(1).nombreUsuario());
        assertEquals("AUXILIAR", resultado.get(1).rol());

        verify(usuarioRepository, times(1)).findAll();
    }

    @Test
    void deberiaRetornarListaVaciaCuandoNoHayUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(List.of());

        List<UsuarioListItemResponse> resultado = usuarioService.listarUsuarios();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(usuarioRepository, times(1)).findAll();
    }

    @Test
    void deberiaCrearUsuarioCorrectamente() {
        when(usuarioRepository.existsByNombreUsuario("jperez")).thenReturn(false);
        when(rolRepository.findByNombre("AUXILIAR")).thenReturn(Optional.of(rolAuxiliar));
        when(estadoUsuarioRepository.findByNombre("ACTIVO")).thenReturn(Optional.of(estadoActivo));
        when(passwordEncoder.encode("Admin123*")).thenReturn("HASH_TEST");

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(10L);
            return usuario;
        });

        UsuarioListItemResponse response = usuarioService.crearUsuario(request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Juan Pérez", response.nombreCompleto());
        assertEquals("jperez", response.nombreUsuario());
        assertEquals("AUXILIAR", response.rol());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        Usuario usuarioGuardado = captor.getValue();
        assertEquals("Juan Pérez", usuarioGuardado.getNombreCompleto());
        assertEquals("jperez", usuarioGuardado.getNombreUsuario());
        assertEquals("HASH_TEST", usuarioGuardado.getContrasenaHash());
        assertEquals(rolAuxiliar, usuarioGuardado.getRol());
        assertEquals(estadoActivo, usuarioGuardado.getEstadoUsuario());
        assertNotNull(usuarioGuardado.getFechaCreacion());

        verify(usuarioRepository).existsByNombreUsuario("jperez");
        verify(rolRepository).findByNombre("AUXILIAR");
        verify(estadoUsuarioRepository).findByNombre("ACTIVO");
        verify(passwordEncoder).encode("Admin123*");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void deberiaFallarCuandoContrasenasNoCoinciden() {
        CrearUsuarioRequest requestInvalido = new CrearUsuarioRequest(
                "Juan Pérez",
                "jperez",
                "Admin123*",
                "Otra123*",
                "AUXILIAR"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.crearUsuario(requestInvalido)
        );

        assertEquals("La contraseña y su confirmación no coinciden", exception.getMessage());

        verify(usuarioRepository, never()).existsByNombreUsuario(any());
        verify(rolRepository, never()).findByNombre(any());
        verify(estadoUsuarioRepository, never()).findByNombre(any());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deberiaFallarCuandoUsuarioYaExiste() {
        when(usuarioRepository.existsByNombreUsuario("jperez")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.crearUsuario(request)
        );

        assertEquals("Ya existe un usuario con ese nombre de usuario", exception.getMessage());

        verify(usuarioRepository).existsByNombreUsuario("jperez");
        verify(rolRepository, never()).findByNombre(any());
        verify(estadoUsuarioRepository, never()).findByNombre(any());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deberiaFallarCuandoRolNoEsPermitido() {
        CrearUsuarioRequest requestInvalido = new CrearUsuarioRequest(
                "Juan Pérez",
                "jperez",
                "Admin123*",
                "Admin123*",
                "SUPERVISOR"
        );

        when(usuarioRepository.existsByNombreUsuario("jperez")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.crearUsuario(requestInvalido)
        );

        assertEquals("El rol debe ser ADMINISTRADOR o AUXILIAR", exception.getMessage());

        verify(usuarioRepository).existsByNombreUsuario("jperez");
        verify(rolRepository, never()).findByNombre(any());
        verify(estadoUsuarioRepository, never()).findByNombre(any());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deberiaFallarCuandoRolNoExisteEnBaseDeDatos() {
        when(usuarioRepository.existsByNombreUsuario("jperez")).thenReturn(false);
        when(rolRepository.findByNombre("AUXILIAR")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.crearUsuario(request)
        );

        assertEquals("El rol indicado no existe en el sistema", exception.getMessage());

        verify(usuarioRepository).existsByNombreUsuario("jperez");
        verify(rolRepository).findByNombre("AUXILIAR");
        verify(estadoUsuarioRepository, never()).findByNombre(any());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deberiaFallarCuandoEstadoActivoNoExiste() {
        when(usuarioRepository.existsByNombreUsuario("jperez")).thenReturn(false);
        when(rolRepository.findByNombre("AUXILIAR")).thenReturn(Optional.of(rolAuxiliar));
        when(estadoUsuarioRepository.findByNombre("ACTIVO")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.crearUsuario(request)
        );

        assertEquals("No existe el estado de usuario ACTIVO", exception.getMessage());

        verify(usuarioRepository).existsByNombreUsuario("jperez");
        verify(rolRepository).findByNombre("AUXILIAR");
        verify(estadoUsuarioRepository).findByNombre("ACTIVO");
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deberiaEditarUsuarioSinCambiarContrasena() {
        EditarUsuarioRequest request = new EditarUsuarioRequest(
                "Juan Carlos Pérez",
                "jperez.actualizado",
                "",
                "",
                "ADMINISTRADOR"
        );

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.existsByNombreUsuario("jperez.actualizado")).thenReturn(false);
        when(rolRepository.findByNombre("ADMINISTRADOR")).thenReturn(Optional.of(rolAdministrador));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioListItemResponse response = usuarioService.editarUsuario(10L, request);

        assertEquals(10L, response.id());
        assertEquals("Juan Carlos Pérez", response.nombreCompleto());
        assertEquals("jperez.actualizado", response.nombreUsuario());
        assertEquals("ADMINISTRADOR", response.rol());

        assertEquals("HASH_OLD", usuarioExistente.getContrasenaHash());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void deberiaEditarUsuarioYCambiarContrasena() {
        EditarUsuarioRequest request = new EditarUsuarioRequest(
                "Juan Carlos Pérez",
                "jperez",
                "Nueva123*",
                "Nueva123*",
                "AUXILIAR"
        );

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioExistente));
        when(rolRepository.findByNombre("AUXILIAR")).thenReturn(Optional.of(rolAuxiliar));
        when(passwordEncoder.encode("Nueva123*")).thenReturn("HASH_NEW");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioListItemResponse response = usuarioService.editarUsuario(10L, request);

        assertEquals("HASH_NEW", usuarioExistente.getContrasenaHash());
        assertEquals("Juan Carlos Pérez", response.nombreCompleto());
        assertEquals("jperez", response.nombreUsuario());
        assertEquals("AUXILIAR", response.rol());

        verify(passwordEncoder).encode("Nueva123*");
    }

    @Test
    void deberiaFallarSiUsuarioNoExiste() {
        EditarUsuarioRequest request = new EditarUsuarioRequest(
                "Juan Carlos Pérez",
                "jperez",
                "",
                "",
                "AUXILIAR"
        );

        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.editarUsuario(99L, request)
        );

        assertEquals("El usuario no existe", ex.getMessage());
    }

    @Test
    void deberiaFallarSiNuevoUsernameYaExiste() {
        EditarUsuarioRequest request = new EditarUsuarioRequest(
                "Juan Carlos Pérez",
                "usuario.duplicado",
                "",
                "",
                "AUXILIAR"
        );

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioExistente));
        when(usuarioRepository.existsByNombreUsuario("usuario.duplicado")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.editarUsuario(10L, request)
        );

        assertEquals("Ya existe un usuario con ese nombre de usuario", ex.getMessage());
    }

    @Test
    void deberiaPermitirEditarSiUsernameEsElMismoActual() {
        EditarUsuarioRequest request = new EditarUsuarioRequest(
                "Juan Actualizado",
                "jperez",
                "",
                "",
                "AUXILIAR"
        );

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioExistente));
        when(rolRepository.findByNombre("AUXILIAR")).thenReturn(Optional.of(rolAuxiliar));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioListItemResponse response = usuarioService.editarUsuario(10L, request);

        assertEquals("Juan Actualizado", response.nombreCompleto());
        verify(usuarioRepository, never()).existsByNombreUsuario(any());
    }

    @Test
    void deberiaFallarSiRolNoEsPermitido() {
        EditarUsuarioRequest request = new EditarUsuarioRequest(
                "Juan Actualizado",
                "jperez",
                "",
                "",
                "SUPERVISOR"
        );

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioExistente));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.editarUsuario(10L, request)
        );

        assertEquals("El rol debe ser ADMINISTRADOR o AUXILIAR", ex.getMessage());
    }

    @Test
    void deberiaFallarSiRolNoExisteEnBD() {
        EditarUsuarioRequest request = new EditarUsuarioRequest(
                "Juan Actualizado",
                "jperez",
                "",
                "",
                "ADMINISTRADOR"
        );

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioExistente));
        when(rolRepository.findByNombre("ADMINISTRADOR")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.editarUsuario(10L, request)
        );

        assertEquals("El rol indicado no existe en el sistema", ex.getMessage());
    }

    @Test
    void deberiaFallarSiSoloSeInformaUnaContrasena() {
        EditarUsuarioRequest request = new EditarUsuarioRequest(
                "Juan Actualizado",
                "jperez",
                "Nueva123*",
                "",
                "AUXILIAR"
        );

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioExistente));
        when(rolRepository.findByNombre("AUXILIAR")).thenReturn(Optional.of(rolAuxiliar));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.editarUsuario(10L, request)
        );

        assertEquals("La contraseña y su confirmación son obligatorias cuando se desea cambiar la contraseña", ex.getMessage());
    }

    @Test
    void deberiaFallarSiContrasenasNoCoinciden() {
        EditarUsuarioRequest request = new EditarUsuarioRequest(
                "Juan Actualizado",
                "jperez",
                "Nueva123*",
                "Otra123*",
                "AUXILIAR"
        );

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuarioExistente));
        when(rolRepository.findByNombre("AUXILIAR")).thenReturn(Optional.of(rolAuxiliar));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.editarUsuario(10L, request)
        );

        assertEquals("La contraseña y su confirmación no coinciden", ex.getMessage());
    }

    @Test
    void deberiaEliminarUsuarioCorrectamente() {
        Usuario usuarioAEliminar = new Usuario();
        usuarioAEliminar.setId(20L);
        usuarioAEliminar.setNombreUsuario("auxiliar1");

        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(usuarioAEliminar));

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        usuarioService.eliminarUsuario(20L);

        verify(usuarioRepository).findById(20L);
        verify(usuarioRepository).delete(usuarioAEliminar);
    }

    @Test
    void deberiaFallarAlEliminarCuandoUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.eliminarUsuario(99L)
        );

        assertEquals("El usuario no existe", ex.getMessage());
        verify(usuarioRepository).findById(99L);
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void noDeberiaPermitirAutoEliminacion() {
        Usuario usuarioAEliminar = new Usuario();
        usuarioAEliminar.setId(1L);
        usuarioAEliminar.setNombreUsuario("admin");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioAEliminar));

        Authentication authentication = new UsernamePasswordAuthenticationToken("admin", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.eliminarUsuario(1L)
        );

        assertEquals("No puedes eliminar tu propio usuario", ex.getMessage());
        verify(usuarioRepository).findById(1L);
        verify(usuarioRepository, never()).delete(any());
    }
}