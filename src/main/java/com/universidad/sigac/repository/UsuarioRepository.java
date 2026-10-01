package com.universidad.sigac.repository;

import com.universidad.sigac.entity.Usuario;
import com.universidad.sigac.enums.RolNombre;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByIdAndActivoTrue(Long id);

    long countByActivo(boolean activo);

    Page<Usuario> findByNombreContainingIgnoreCaseOrEmailContainingIgnoreCase(String nombre, String email, Pageable pageable);

    /** Usuarios activos de una Facultad con alguno de los roles dados (quórum y destinatarios de correos). */
    @Query("select distinct u from Usuario u join u.roles r where u.facultad.id = :facultadId and u.activo = true "
            + "and r in :roles order by u.nombre")
    List<Usuario> findActivosPorFacultadYRoles(@Param("facultadId") Long facultadId, @Param("roles") Collection<RolNombre> roles);

    @Query("select distinct u from Usuario u join u.roles r where u.activo = true and r in :roles")
    List<Usuario> findActivosPorRoles(@Param("roles") Collection<RolNombre> roles);
}
