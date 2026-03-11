package com.univalle.parkingmanagementservice.usuario.service.serviceImpl;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import com.univalle.parkingmanagementservice.auth.entities.EstadoUsuario;
import com.univalle.parkingmanagementservice.auth.entities.Rol;
import com.univalle.parkingmanagementservice.auth.entities.Usuario;
import com.univalle.parkingmanagementservice.auth.repositories.EstadoUsuarioRepository;
import com.univalle.parkingmanagementservice.auth.repositories.RolRepository;
import com.univalle.parkingmanagementservice.auth.repositories.UsuarioRepository;
import com.univalle.parkingmanagementservice.usuario.dto.CrearUsuarioRequest;
import com.univalle.parkingmanagementservice.usuario.dto.UsuarioListItemResponse;
import com.univalle.parkingmanagementservice.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    private static final String ROL_AUXILIAR = "AUXILIAR";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final EstadoUsuarioRepository estadoUsuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioListItemResponse> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Usuario::getNombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UsuarioListItemResponse crearUsuario(CrearUsuarioRequest request) {
        validarContrasenas(request.contrasena(), request.confirmacionContrasena());

        String nombreUsuarioNormalizado = request.nombreUsuario().trim();
        validarUsuarioNoExiste(nombreUsuarioNormalizado);

        String nombreRolNormalizado = request.rol().trim().toUpperCase();
        validarRolPermitido(nombreRolNormalizado);

        Rol rol = rolRepository.findByNombre(nombreRolNormalizado)
                .orElseThrow(() -> new IllegalArgumentException("El rol indicado no existe en el sistema"));

        EstadoUsuario estadoActivo = estadoUsuarioRepository.findByNombre("ACTIVO")
                .orElseThrow(() -> new IllegalArgumentException("No existe el estado de usuario ACTIVO"));

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(request.nombreCompleto().trim());
        usuario.setNombreUsuario(nombreUsuarioNormalizado);
        usuario.setContrasenaHash(passwordEncoder.encode(request.contrasena()));
        usuario.setRol(rol);
        usuario.setEstadoUsuario(estadoActivo);
        usuario.setFechaCreacion(OffsetDateTime.now());

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        return new UsuarioListItemResponse(
                usuarioGuardado.getId(),
                usuarioGuardado.getNombreCompleto(),
                usuarioGuardado.getNombreUsuario(),
                usuarioGuardado.getRol().getNombre()
        );
    }

    private UsuarioListItemResponse toResponse(Usuario usuario) {
        return new UsuarioListItemResponse(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getNombreUsuario(),
                usuario.getRol().getNombre()
        );
    }

    private void validarContrasenas(String contrasena, String confirmacionContrasena) {
        if (!contrasena.equals(confirmacionContrasena)) {
            throw new IllegalArgumentException("La contraseña y su confirmación no coinciden");
        }
    }

    private void validarUsuarioNoExiste(String nombreUsuario) {
        if (usuarioRepository.existsByNombreUsuario(nombreUsuario)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre de usuario");
        }
    }

    private void validarRolPermitido(String rol) {
        if (!ROL_ADMINISTRADOR.equals(rol) && !ROL_AUXILIAR.equals(rol)) {
            throw new IllegalArgumentException("El rol debe ser ADMINISTRADOR o AUXILIAR");
        }
    }

}
