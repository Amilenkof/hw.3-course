package ru.hogwarts.school.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.hogwarts.school.exeption.AvatarNotFoundException;
import ru.hogwarts.school.exeption.StudentNotFoundException;
import ru.hogwarts.school.model.Avatar;
import ru.hogwarts.school.model.Student;
import ru.hogwarts.school.repository.AvatarRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

@Service
@Scope("singleton")
//@Transactional
public class AvatarService {
    @Value("${path.to.avatars.folder}")
    private String pathAdress;
    private final AvatarRepository avatarRepository;
    //    private final StudentRepository studentRepository;
    private final StudentService studentService;


    public AvatarService(AvatarRepository avatarRepository, StudentService studentService) {
        this.avatarRepository = avatarRepository;
        this.studentService = studentService;

    }


    public void uploadAvatar(Long studentId, MultipartFile avatarFile) throws IOException {
        Student student = studentService.read(studentId).orElseThrow(() -> new StudentNotFoundException("Указанный студент не найден"));//находим студента с указанным id

        Path path = Path.of(pathAdress, studentId + "." + getExtensions(avatarFile.getOriginalFilename())); //создаем путь к нему (2 параметра 1-папка где лежит будет создан файл, 2 как называть новый файл) , строка из проперти,
        // указывает в какую папку создаем директорию, потом получаем расширение файла и склеиваем его с id- чтобы было уникальное значение

        Files.createDirectories(path.getParent());//проверяем есть ли папки по адресу ( в проперти  @Value("${path.to.avatars.folder}") private String pathAdress;) если нет он их создаст
        Files.deleteIfExists(path);//проверяем есть ли уже такой файл, если есть удаляем


        try (InputStream inputStream = avatarFile.getInputStream();
             OutputStream outputStream = Files.newOutputStream(path, CREATE_NEW);) {
            inputStream.transferTo(outputStream);
        }
        System.out.println("student = " + student);
        Avatar avatar = avatarRepository.findAvatarByStudent_Id(studentId).orElse(new Avatar());// аватары создаются через сеттеры, мы получаем из SQL запроса какойто аватар по id студента, если он null
        avatar.setStudent(student);
        avatar.setFilePath(path.toString());
        avatar.setFileSize(avatarFile.getSize());
        avatar.setMediaType(avatarFile.getContentType());
        avatar.setData(avatarFile.getBytes());
        avatarRepository.save(avatar);


    }


    private String getExtensions(String s) {
        return s.substring(s.lastIndexOf(".") + 1);
    }

    public Avatar findAvatarByStudentId(Long studentId) {
        return avatarRepository.findAvatarByStudent_Id(studentId).orElseThrow(() -> new AvatarNotFoundException("Аватар с указанным id не найден"));
    }

    public List<Avatar> getAllAvatars(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return avatarRepository.findAll(pageRequest).getContent();
//        return allAvatars.stream().map(Avatar::getData).toList();

    }
}
